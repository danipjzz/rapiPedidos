
package com.rapipedido.api.payments;

import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class PaymentService {

    private static final Logger logger =
            LoggerFactory.getLogger(PaymentService.class);

    private static final String ERROR_STATUS = "ERROR";

    private static final long TIMEOUT_SECONDS = 5;
    private static final long RETRY_DELAY_SECONDS = 10;

    private final PaymentGateway paymentGateway;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    private final AtomicBoolean circuitOpen =
            new AtomicBoolean(false);

    public PaymentService(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public PaymentResponse processPayment(PaymentRequest request) {

        if (circuitOpen.get()) {
            return paymentPending();
        }

        Future<Object> future = executor.submit(
                () -> paymentGateway.processPayment(request)
        );

        try {
            Object data = future.get(
                    TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );

            return new PaymentResponse(
                    "SUCCESS",
                    "Pago procesado correctamente",
                    data,
                    null
            );

        } catch (TimeoutException e) {

            future.cancel(true);

            if (circuitOpen.compareAndSet(false, true)) {
                scheduleRetry(request);
            }

            return paymentPending();

        } catch (ExecutionException e) {

            logger.error("Error al procesar el pago", e.getCause());

            return new PaymentResponse(
                    ERROR_STATUS,
                    "No fue posible procesar el pago",
                    null,
                    "PAYMENT_PROCESSING_ERROR"
            );

        } catch (InterruptedException e) {

            future.cancel(true);
            Thread.currentThread().interrupt();

            return new PaymentResponse(
                    ERROR_STATUS,
                    "La operación de pago fue interrumpida",
                    null,
                    "PAYMENT_INTERRUPTED"
            );
        }
    }

    private void scheduleRetry(PaymentRequest request) {

        scheduler.schedule(() -> {

            Future<Object> retryFuture = executor.submit(
                    () -> paymentGateway.processPayment(request)
            );

            try {
                retryFuture.get(
                        TIMEOUT_SECONDS,
                        TimeUnit.SECONDS
                );

                circuitOpen.set(false);

                logger.info(
                        "Pago reintentado correctamente. Circuito cerrado."
                );

            } catch (TimeoutException e) {

                retryFuture.cancel(true);

                logger.warn(
                        "El reintento superó el tiempo máximo. Se programará otro."
                );

                scheduleRetry(request);

            } catch (ExecutionException e) {

                logger.warn(
                        "El reintento falló. Se programará otro intento.",
                        e.getCause()
                );

                scheduleRetry(request);

            } catch (InterruptedException e) {

                retryFuture.cancel(true);
                Thread.currentThread().interrupt();

                logger.warn("El reintento fue interrumpido.");

            }

        }, RETRY_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    private PaymentResponse paymentPending() {

        return new PaymentResponse(
                ERROR_STATUS,
                "La pasarela externa no respondió a tiempo. "
                        + "El pago está en estado pendiente y se reintentará "
                        + "de forma asíncrona.",
                null,
                "PAYMENT_GATEWAY_TIMEOUT"
        );
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
        scheduler.shutdownNow();
    }
}
