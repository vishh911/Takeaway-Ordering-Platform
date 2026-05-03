package com.campuseateries.model.payment;

/** Strategy-style hook for plugging payment gateways (pure polymorphism abstraction). */
public interface IPaymentSimulator {

    /** Human readable label surfaced in consoles/REST clients. */
    String channelName();

    /**
     * @return probabilistic-ish success flag for demos; real gateways would unwrap JSON instead.
     */
    boolean authorize(double amountUsd);
}
