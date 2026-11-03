package com.capsule.insurance.payment.adapter;

import static org.assertj.core.api.Assertions.*;

import com.capsule.insurance.payment.application.port.PremiumPaymentGateway;
import com.capsule.insurance.payment.domain.GatewayPaymentResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PaymentConcurrencyLimitTest {
    @Test
    void sharesCapacityBetweenApprovalAndInquiryWithoutWaitingOrOpeningCircuit() throws Exception {
        var entered=new CountDownLatch(2);var release=new CountDownLatch(1);var called=new AtomicInteger();
        PremiumPaymentGateway remote=new PremiumPaymentGateway(){
            private GatewayPaymentResult call(String key){
                called.incrementAndGet();entered.countDown();
                try {if(!release.await(5,TimeUnit.SECONDS))throw new IllegalStateException("release timed out");}
                catch(InterruptedException ex){Thread.currentThread().interrupt();throw new IllegalStateException(ex);}
                return GatewayPaymentResult.paid(key,"TEST-PAID");
            }
            public GatewayPaymentResult confirm(ConfirmCommand c){return call(c.providerPaymentKey());}
            public GatewayPaymentResult inquire(InquiryCommand c){return call(c.providerPaymentKey());}
        };
        var gateway=gateway(remote,2);
        try(var workers=Executors.newFixedThreadPool(2)){
            var approval=workers.submit(()->gateway.confirm(command("first")));
            var inquiry=workers.submit(()->gateway.inquire(inquiry("second")));
            try{
                assertThat(entered.await(3,TimeUnit.SECONDS)).isTrue();
                assertThat(gateway.confirm(command("busy-confirm")).errorCode()).isEqualTo("PAYMENT_INTERFACE_BUSY");
                assertThat(gateway.inquire(inquiry("busy-inquiry")).errorCode()).isEqualTo("PAYMENT_INTERFACE_BUSY");
                assertThat(called.get()).isEqualTo(2);
                assertThat(gateway.currentStatus().consecutiveTimeouts()).isZero();
                release.countDown();
                assertThat(approval.get(3,TimeUnit.SECONDS).status()).isEqualTo("PAID");
                assertThat(inquiry.get(3,TimeUnit.SECONDS).status()).isEqualTo("PAID");
                assertThat(gateway.confirm(command("after-release")).status()).isEqualTo("PAID");
            } finally {release.countDown();}
        }
    }
    @Test
    void releasesApprovalAndInquiryCapacityEvenWhenDelegateThrows() {
        var calls=new AtomicInteger();
        PremiumPaymentGateway remote=new PremiumPaymentGateway(){
            GatewayPaymentResult call(String key){if(calls.incrementAndGet()%2==1)throw new IllegalStateException("TEST_FAILURE");return GatewayPaymentResult.paid(key,"TX");}
            public GatewayPaymentResult confirm(ConfirmCommand c){return call(c.providerPaymentKey());}
            public GatewayPaymentResult inquire(InquiryCommand c){return call(c.providerPaymentKey());}
        };
        var gateway=gateway(remote,1);
        assertThat(gateway.confirm(command("throw-confirm")).errorCode()).isEqualTo("PAYMENT_INTERFACE_CONFIRM_ERROR");
        assertThat(gateway.confirm(command("recover-confirm")).status()).isEqualTo("PAID");
        assertThat(gateway.inquire(inquiry("throw-inquiry")).errorCode()).isEqualTo("PAYMENT_INTERFACE_INQUIRY_ERROR");
        assertThat(gateway.inquire(inquiry("recover-inquiry")).status()).isEqualTo("PAID");
    }
    @Test
    void rejectsNonPositiveCapacity(){assertThatThrownBy(()->gateway(new FakePremiumPaymentGateway(),0)).isInstanceOf(IllegalArgumentException.class);}
    @Test
    void admissionPermitIsSingleUseAndCloseCannotLeakExtraCapacity(){
        var gateway=gateway(new FakePremiumPaymentGateway(),1);
        var permit=gateway.tryReserveConfirmation();
        assertThat(permit).isNotNull();
        permit.confirm(command("fake-paid-one-use"));
        assertThatThrownBy(()->permit.confirm(command("fake-paid-second-use"))).isInstanceOf(IllegalStateException.class);
        permit.close();permit.close();
        var held=gateway.tryReserveConfirmation();
        assertThat(held).isNotNull();
        try{assertThat(gateway.tryReserveConfirmation()).isNull();}finally{held.close();}
    }
    private static JournaledPremiumPaymentGateway gateway(PremiumPaymentGateway remote,int limit){
        return new JournaledPremiumPaymentGateway(remote,message->{},new ObjectMapper(),Clock.systemUTC(),3,Duration.ofSeconds(30),new InMemoryPaymentCircuitStateStore(Clock.systemUTC()),limit);
    }
    private static PremiumPaymentGateway.ConfirmCommand command(String id){return new PremiumPaymentGateway.ConfirmCommand("ORDER-"+id,id,BigDecimal.valueOf(29900),"KRW",id);}
    private static PremiumPaymentGateway.InquiryCommand inquiry(String id){return new PremiumPaymentGateway.InquiryCommand("ORDER-"+id,id,BigDecimal.valueOf(29900),"KRW");}
}
