package com.wimone.enjoytix.agent.trade;

import com.wimone.enjoytix.agent.auth.AgentUserContextHolder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPurchaseDraftService implements PurchaseDraftService {
    private final Duration lifetime;
    private final TicketPurchaseQuoteService quoteService;
    private final Map<String, PurchaseDraft> drafts = new ConcurrentHashMap<>();
    private final Map<String, String> confirmations = new ConcurrentHashMap<>();

    public InMemoryPurchaseDraftService(Duration lifetime, TicketPurchaseQuoteService quoteService) {
        if (lifetime == null || lifetime.isZero() || lifetime.isNegative()) {
            throw new IllegalArgumentException("lifetime must be positive");
        }
        this.lifetime = lifetime;
        this.quoteService = quoteService;
    }

    @Override
    public PurchaseDraft createDraft(PurchaseDraftRequest request) {
        Long userId = AgentUserContextHolder.requireCurrent().userId();
        PurchaseQuote quote = quoteService.quote(request);
        Instant expiresAt = Instant.now().plus(lifetime);
        PurchaseDraft draft = new PurchaseDraft(UUID.randomUUID().toString(), userId, request.showId(),
                request.categoryId(), quote.seatIds(), request.quantity(), quote.unitPrice(),
                quote.totalAmount(), fingerprint(request, quote), expiresAt);
        drafts.put(draft.draftId(), draft);
        return draft;
    }

    @Override
    public PurchaseConfirmation issueConfirmation(String draftId) {
        PurchaseDraft draft = ownedDraft(draftId);
        if (expired(draft.expiresAt())) return new PurchaseConfirmation("", draftId, draft.expiresAt());
        String token = UUID.randomUUID().toString() + UUID.randomUUID();
        confirmations.put(hash(token), draft.draftId());
        return new PurchaseConfirmation(token, draft.draftId(), draft.expiresAt());
    }

    @Override
    public boolean confirm(String token, String draftId, PurchaseDraftRequest request) {
        if (token == null || draftId == null) return false;
        PurchaseDraft draft = drafts.get(draftId);
        if (draft == null || !draft.userId().equals(AgentUserContextHolder.requireCurrent().userId()) || expired(draft.expiresAt())) {
            return false;
        }
        String tokenHash = hash(token);
        PurchaseQuote quote = quoteService.quote(request);
        if (!draft.parameterFingerprint().equals(fingerprint(request, quote))) {
            confirmations.remove(tokenHash, draftId);
            return false;
        }
        return confirmations.remove(tokenHash, draftId);
    }

    @Override
    public PurchaseDraft consume(String token, String draftId) {
        if (token == null || draftId == null) throw new IllegalArgumentException("confirmation is invalid");
        PurchaseDraft draft = drafts.get(draftId);
        if (draft == null || !draft.userId().equals(AgentUserContextHolder.requireCurrent().userId()) || expired(draft.expiresAt())) {
            throw new IllegalArgumentException("confirmation is invalid");
        }
        if (!confirmations.remove(hash(token), draftId)) {
            throw new IllegalArgumentException("confirmation is invalid");
        }
        return draft;
    }

    private PurchaseDraft ownedDraft(String draftId) {
        PurchaseDraft draft = drafts.get(draftId);
        if (draft == null || !draft.userId().equals(AgentUserContextHolder.requireCurrent().userId())) {
            throw new IllegalArgumentException("draft does not belong to current user");
        }
        return draft;
    }

    private boolean expired(Instant expiresAt) { return !Instant.now().isBefore(expiresAt); }

    private String fingerprint(PurchaseDraftRequest request, PurchaseQuote quote) {
        return hash(request.showId() + "|" + request.categoryId() + "|" + request.seatIds()
                + "|" + request.quantity() + "|" + quote.unitPrice().stripTrailingZeros().toPlainString());
    }

    private String hash(String value) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
