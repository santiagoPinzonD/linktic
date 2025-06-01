package com.linktic.inventario_service.event;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.linktic.inventario_service.entity.InventarioCambiadoEvent;
import com.linktic.inventario_service.events.InventarioEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class InventarioEventListenerTest {

    private InventarioEventListener eventListener;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        eventListener = new InventarioEventListener();

        Logger logger = (Logger) LoggerFactory.getLogger(InventarioEventListener.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @Test
    @DisplayName("Evento normal debe registrar información básica")
    void handleInventarioCambiado_EventoNormal() {
        // Given
        InventarioCambiadoEvent evento = InventarioCambiadoEvent.builder()
                .productoId(1L)
                .cantidadAnterior(100)
                .cantidadNueva(80)
                .tipoOperacion("COMPRA")
                .timestamp(LocalDateTime.now())
                .build();

        // When
        eventListener.handleInventarioCambiado(evento);

        // Then
        assertThat(listAppender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anyMatch(msg -> msg.contains("EVENTO DE INVENTARIO"))
                .anyMatch(msg -> msg.contains("Producto ID: 1"))
                .anyMatch(msg -> msg.contains("Cantidad anterior: 100"))
                .anyMatch(msg -> msg.contains("Cantidad nueva: 80"))
                .anyMatch(msg -> msg.contains("Tipo de operación: COMPRA"));
    }

    @Test
    @DisplayName("Stock bajo debe generar alerta WARNING")
    void handleInventarioCambiado_StockBajo() {
        // Given
        InventarioCambiadoEvent evento = InventarioCambiadoEvent.builder()
                .productoId(1L)
                .cantidadAnterior(15)
                .cantidadNueva(8)
                .tipoOperacion("COMPRA")
                .timestamp(LocalDateTime.now())
                .build();

        // When
        eventListener.handleInventarioCambiado(evento);

        // Then
        assertThat(listAppender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anyMatch(msg -> msg.contains("¡ALERTA! Stock bajo"))
                .anyMatch(msg -> msg.contains("8 unidades"));
    }

    @Test
    @DisplayName("Stock crítico debe generar alerta CRÍTICA")
    void handleInventarioCambiado_StockCritico() {
        // Given
        InventarioCambiadoEvent evento = InventarioCambiadoEvent.builder()
                .productoId(1L)
                .cantidadAnterior(10)
                .cantidadNueva(3)
                .tipoOperacion("COMPRA")
                .timestamp(LocalDateTime.now())
                .build();

        // When
        eventListener.handleInventarioCambiado(evento);

        // Then
        assertThat(listAppender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anyMatch(msg -> msg.contains("¡ALERTA CRÍTICA! Stock MUY BAJO"))
                .anyMatch(msg -> msg.contains("3 unidades"));
    }
}
