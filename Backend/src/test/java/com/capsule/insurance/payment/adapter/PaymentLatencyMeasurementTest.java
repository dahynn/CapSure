package com.capsule.insurance.payment.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.capsule.insurance.payment.application.port.PremiumPaymentGateway;
import com.capsule.insurance.payment.domain.GatewayPaymentResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.net.ssl.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Opt-in loopback TLS experiment. This is not production TPS/SLA or a real Toss request. */
@Tag("payment-latency")
class PaymentLatencyMeasurementTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int WORKERS = 8;
    private static final int NORMAL_TASKS = 20;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);

    @Test
    void measuresNormalTaskQueueingWhilePgRequestsTimeOut() throws Exception {
        int capacity = Integer.getInteger("capsure.payment-latency.capacity", 2);
        int expectedCalls = Math.min(WORKERS, capacity);
        Path temporary = Files.createTempDirectory("capsure-loopback-pg-");
        SSLContext tls = createTls(temporary);
        var phase = new AtomicReference<Phase>();
        var serverWorkers = Executors.newCachedThreadPool();
        HttpsServer server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(tls));
        server.setExecutor(serverWorkers);
        server.createContext("/v1/payments/confirm", exchange -> {
            Phase current = phase.get();
            boolean measured = current.measured;
            if (measured) {
                int active = current.active.incrementAndGet();
                current.peak.accumulateAndGet(active, Math::max);
                current.remoteCalls.incrementAndGet(); current.entered.countDown();
            }
            try {
                var request = JSON.readTree(exchange.getRequestBody());
                if (measured && !current.release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("PG hold timed out");
                byte[] body = JSON.writeValueAsBytes(Map.of("status", "DONE", "orderId", request.path("orderId").asText(),
                        "paymentKey", request.path("paymentKey").asText(), "totalAmount", 29900, "currency", "KRW"));
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length); exchange.getResponseBody().write(body);
            } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            catch (java.io.IOException ignored) { /* Client has timed out; expected in measured phase. */ }
            finally { exchange.close(); if (measured) current.active.decrementAndGet(); }
        });
        server.start();
        var client = HttpClient.newBuilder().sslContext(tls).connectTimeout(REQUEST_TIMEOUT).build();
        List<Map<String,Object>> trials = new ArrayList<>();
        try {
            for (int trial=0; trial<3; trial++) {
                Phase current = new Phase(expectedCalls); phase.set(current);
                var transport = (TossPremiumPaymentGateway.TossHttpTransport) request -> {
                    try {
                        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                        return new TossPremiumPaymentGateway.TossHttpResponse(response.statusCode(), response.body());
                    } finally { if (current.measured) current.finishedIo.incrementAndGet(); }
                };
                var remote = new TossPremiumPaymentGateway(URI.create("https://127.0.0.1:"+server.getAddress().getPort()),
                        "test_sk_loopback_only", REQUEST_TIMEOUT, false, JSON, transport);
                var gateway = new JournaledPremiumPaymentGateway(remote, message -> {}, JSON,
                        Clock.systemUTC(), 3, Duration.ofSeconds(30),
                        new InMemoryPaymentCircuitStateStore(Clock.systemUTC()), capacity);
                try (var workers = Executors.newFixedThreadPool(WORKERS)) {
                    List<Future<?>> warmups = new ArrayList<>();
                    for (int i=0;i<WORKERS;i++) { int id=i;warmups.add(workers.submit(() -> gateway.confirm(command("warmup-"+id)))); }
                    for (var warmup:warmups) warmup.get(10,TimeUnit.SECONDS);
                    current.measured=true;
                    var start = new CountDownLatch(1);
                    List<Future<GatewayPaymentResult>> payments = new ArrayList<>();
                    for (int i=0;i<WORKERS;i++) { int id=i;payments.add(workers.submit(() -> { start.await();return gateway.confirm(command("load-"+id)); })); }
                    start.countDown();
                    assertThat(current.entered.await(5,TimeUnit.SECONDS)).isTrue();
                    var firstNormalSawFinishedIo = new AtomicInteger(-1);
                    List<Future<Double>> normal = new ArrayList<>();
                    for(int i=0;i<NORMAL_TASKS;i++) {
                        long enqueued=System.nanoTime();
                        normal.add(workers.submit(() -> {
                            firstNormalSawFinishedIo.compareAndSet(-1,current.finishedIo.get());
                            return (System.nanoTime()-enqueued)/1_000_000.0;
                        }));
                    }
                    List<Double> waits=new ArrayList<>();for(var task:normal)waits.add(task.get(10,TimeUnit.SECONDS));
                    Map<String,Long> outcomes=new TreeMap<>();
                    for(var payment:payments) {
                        var result=payment.get(10,TimeUnit.SECONDS);
                        assertThat(result.status()).isEqualTo("UNKNOWN");
                        outcomes.merge(result.errorCode(),1L,Long::sum);
                    }
                    assertThat(current.peak.get()).isLessThanOrEqualTo(expectedCalls);
                    assertThat(outcomes.getOrDefault("PAYMENT_INTERFACE_BUSY", 0L)).isEqualTo((long) WORKERS - expectedCalls);
                    if (capacity < WORKERS) assertThat(firstNormalSawFinishedIo.get()).isZero();
                    else assertThat(firstNormalSawFinishedIo.get()).isPositive();
                    trials.add(Map.of("trial",trial+1,"remote_peak_concurrency",current.peak.get(),
                            "remote_calls",current.remoteCalls.get(),"normal_queue_p50_ms",percentile(waits,0.50),
                            "normal_queue_p95_ms",percentile(waits,0.95),"normal_started_before_first_io_finished",firstNormalSawFinishedIo.get()==0,
                            "outcomes",outcomes));
                } finally { current.release.countDown(); }
            }
            Path output=Path.of(System.getProperty("capsure.payment-latency.output","build/payment-latency-protected.json"));
            Files.createDirectories(output.toAbsolutePath().getParent());
            JSON.writerWithDefaultPrettyPrinter().writeValue(output.toFile(),Map.of("application_worker_model",WORKERS,
                    "pg_concurrent_call_limit",capacity,"concurrent_payment_tasks",WORKERS,"normal_tasks_per_trial",NORMAL_TASKS,"request_timeout_ms",REQUEST_TIMEOUT.toMillis(),
                    "transport","Loopback HTTPS, runtime-generated TLS certificate; real JDK HttpClient request timeout",
                    "limits","Controlled fixed worker pool model. Journal/DB overhead excluded. Queue waits are not application API latency or production SLA.","trials",trials));
            System.out.println("PAYMENT_LATENCY_MEASUREMENT="+output.toAbsolutePath());
        } finally { if(phase.get()!=null)phase.get().release.countDown();client.close();server.stop(0);serverWorkers.shutdownNow(); }
    }
    private static PremiumPaymentGateway.ConfirmCommand command(String id) {
        return new PremiumPaymentGateway.ConfirmCommand("ORDER-"+id,"PAY-"+id,BigDecimal.valueOf(29900),"KRW","KEY-"+id);
    }
    private static double percentile(List<Double> values,double fraction) {
        var sorted=new ArrayList<>(values);Collections.sort(sorted);return sorted.get((int)Math.ceil(sorted.size()*fraction)-1);
    }
    private static SSLContext createTls(Path directory) throws Exception {
        Path store=directory.resolve("loopback.p12");String password="loopback-fixture-only";
        var command=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","keytool").toString(),
                "-genkeypair","-alias","loopback","-keyalg","RSA","-keysize","2048","-validity","1",
                "-dname","CN=localhost","-ext","SAN=DNS:localhost,IP:127.0.0.1","-storetype","PKCS12",
                "-keystore",store.toString(),"-storepass",password,"-keypass",password,"-noprompt")
                .redirectErrorStream(true).redirectOutput(directory.resolve("keytool.log").toFile()).start();
        if(!command.waitFor(20,TimeUnit.SECONDS)||command.exitValue()!=0)throw new IllegalStateException("fixture certificate generation failed");
        KeyStore keys=KeyStore.getInstance("PKCS12");try(var input=Files.newInputStream(store)){keys.load(input,password.toCharArray());}
        var keyManager=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());keyManager.init(keys,password.toCharArray());
        var trust=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());trust.init(keys);
        SSLContext tls=SSLContext.getInstance("TLS");tls.init(keyManager.getKeyManagers(),trust.getTrustManagers(),null);return tls;
    }
    private static final class Phase {
        volatile boolean measured;
        final AtomicInteger active=new AtomicInteger(),peak=new AtomicInteger(),remoteCalls=new AtomicInteger(),finishedIo=new AtomicInteger();
        final CountDownLatch entered,release=new CountDownLatch(1);
        Phase(int expected){entered=new CountDownLatch(expected);}
    }
}
