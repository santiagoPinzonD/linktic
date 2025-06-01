package com.linktic.inventario_service.events;

import com.linktic.inventario_service.entity.InventarioCambiadoEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InventarioEventListener {

    @EventListener
    public void handleInventarioCambiado(InventarioCambiadoEvent event) {
        log.info("=== EVENTO DE INVENTARIO ===");
        log.info("Producto ID: {}", event.getProductoId());
        log.info("Cantidad anterior: {}", event.getCantidadAnterior());
        log.info("Cantidad nueva: {}", event.getCantidadNueva());
        log.info("Tipo de operación: {}", event.getTipoOperacion());
        log.info("Timestamp: {}", event.getTimestamp());

        log.info("==========================");

        if (event.getCantidadNueva() < 5) {
            log.error("¡ALERTA CRÍTICA! Stock MUY BAJO: {} unidades", event.getCantidadNueva());
        } else if (event.getCantidadNueva() < 10) {
            log.warn("¡ALERTA! Stock bajo para producto: {} con {} unidades", event.getProductoId(), event.getCantidadNueva());
        }
    }
}

