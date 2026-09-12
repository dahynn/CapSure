# Cancer insurance flow — design QA

- Source visual truth: `/var/folders/s2/8kkl7f5d57z0b0qj5k0vt3bc0000gn/T/TemporaryItems/NSIRD_screencaptureui_L1OKLJ/스크린샷 2026-09-12 오후 11.40.34.png`
- Implementation: `http://localhost:4173/cancer-insurance` in Codex in-app Browser tab 1
- Implementation screenshot: inline browser capture produced during this task; repository screenshot file was not created
- Viewports: 390 × 844 CSS px for the first responsive pass; 542 × 837 CSS px for the final full-page pass
- Source pixels: 730 × 162 px, cropped and perspective-rotated phone mockup
- Implementation comparison crop: 375 × 56 px after the browser scrollbar; unrotated app content
- State: source shows step 2, implementation capture shows step 1 because a direct deep link without quote context redirects to the product step. The comparison therefore covers layout, typography, color, and wrapping rather than exact progress value.

## Full-view comparison evidence

The original places six numbered circles, six labels, and connector lines in one narrow row. Every two-syllable Korean label wraps vertically. The revised header shows the current stage, count, next stage, and a single progress bar within the same mobile content width. At 390px there is no horizontal overflow and both text groups remain on one line.

The product, application, payment, policy, claim, and claim-result views now share the same restrained visual language: 20px primary surfaces, 12px option cards, solid navy fills, lighter heading weights, and unboxed section icons. Decorative blur shapes, presentation-style gradients, oversized shadows, and repeated `font-black` treatments were removed from the customer flow.

## Focused region comparison evidence

A focused capture of the progress header and the final product-page capture were compared with the supplied crop in the same browser inspection. The direct product route is the only customer-flow state available without a signed-in quote context; downstream views were checked through source review and a production build without changing their guards or data flow.

## Required fidelity surfaces

- Fonts and typography: existing Pretendard is preserved. Current stage uses 13px/600, metadata uses 11px/500, and `white-space: nowrap` prevents character-by-character Korean wrapping.
- Spacing and layout rhythm: 20px horizontal padding and 12px vertical padding align with the page content. The six-node row is replaced by one compact line and a 3px progress track. Primary panels use 20px radii; repeated option cards use 12px radii.
- Colors and visual tokens: the existing navy and brand blue remain. Secondary copy uses slate tones; customer-flow feature surfaces use solid navy fills without decorative gradients or glow.
- Image quality and assets: this component contains no image assets. Existing app logos and icons are unchanged; no CSS art, emoji, or replacement asset was introduced.
- Copy and content: `가입 진행`, current step, `현재 / 전체`, and `다음` preserve all decision-relevant information with less visual noise.

## Comparison history

1. Initial revision removed the numbered-circle stepper and vertical label wrapping. The first mobile capture showed the next-step label at `slate-600`, which was too subdued against the navy background (P2).
2. The next-step label was raised to `slate-400`. The second 390px capture showed no wrapping, clipping, horizontal overflow, or browser console errors.
3. The full customer journey was normalized to solid surfaces and a calmer type scale. Browser inspection then found the progress summary overlapping the page title (P1).
4. The summary was returned to normal document flow below the global header. The final 542px capture shows the global header, progress summary, page title, product panel, and fixed CTA without overlap or horizontal overflow.
5. The boxed shield icon in the product summary was removed after review because it added decorative emphasis without conveying unique information. The final capture confirms that the insurer, product name, premium, and selected coverage count remain visually distinct without it.

## Findings

- No actionable P0, P1, or P2 issues remain in the visually reachable customer-flow region.
- P3: the exact visual balance on a perspective-rotated portfolio device mockup may vary with the mockup's scale; the app component itself remains stable at the measured mobile width.

## Implementation checklist

- [x] Replace the crowded six-node layout with a compact progress summary.
- [x] Keep current and next stage readable without Korean character wrapping.
- [x] Preserve route behavior and support nested payment/result URLs.
- [x] Unify product, application, payment, policy, claim, and result surfaces.
- [x] Remove decorative gradients, blur blobs, oversized radii, and excessive extra-bold type from the customer flow.
- [x] Check 390px layout and horizontal overflow.
- [x] Re-check final 542px viewport and header stacking.
- [x] Check browser warnings and errors.
- [x] Run frontend tests and production build.

final result: passed
