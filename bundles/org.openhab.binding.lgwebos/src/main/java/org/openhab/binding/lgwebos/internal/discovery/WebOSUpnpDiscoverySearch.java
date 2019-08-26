/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.lgwebos.internal.discovery;

import static org.openhab.binding.lgwebos.internal.WebOSBindingConstants.*;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.jupnp.UpnpService;
import org.jupnp.model.message.header.ServiceTypeHeader;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * WebOS TV does not automatically show up by the normal background scans, as it does not respond to openHABs generic
 * search request. A special ssdp search request for lge-com:service:webos-second-screen:1 is required.
 * This component sends this ssdp search request.
 *
 * @author Sebastian Prehn - Initial contribution
 *
 */
@NonNullByDefault
@Component(service = DiscoveryService.class, immediate = true, configurationPid = "discovery.lgwebos")
public class WebOSUpnpDiscoverySearch extends AbstractDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(WebOSUpnpDiscoverySearch.class);

    private static final int DISCOVERY_TIMEOUT_SECONDS = 10;
    private static final int SEARCH_INTERVAL_SECONDS = 10;
    private static final int SEARCH_START_UP_DELAY_SECONDS = 0;

    @NonNullByDefault({})
    private UpnpService upnpService;

    private @Nullable ScheduledFuture<?> searchJob;

    public WebOSUpnpDiscoverySearch() {
        super(SUPPORTED_THING_TYPES_UIDS, DISCOVERY_TIMEOUT_SECONDS, true);
    }

    @Reference
    public void setUpnpService(UpnpService upnpService) {
        this.upnpService = upnpService;
    }

    public void unsetUpnpService(UpnpService upnpService) {
        this.upnpService = null;
    }

    private void search() {
        logger.trace("Sending Upnp Search Request for {}", UPNP_SERVICE_TYPE);
        upnpService.getControlPoint().search(new ServiceTypeHeader(UPNP_SERVICE_TYPE));
    }

    @Override
    protected void startBackgroundDiscovery() {
        logger.debug("Start LGWebOS device background discovery");
        if (searchJob == null || searchJob.isCancelled()) {
            searchJob = scheduler.scheduleWithFixedDelay(() -> search(), SEARCH_START_UP_DELAY_SECONDS,
                    SEARCH_INTERVAL_SECONDS, TimeUnit.SECONDS);
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        logger.debug("Stop LGWebOS device background discovery");
        if (searchJob != null && !searchJob.isCancelled()) {
            searchJob.cancel(false);
            searchJob = null;
        }
    }

    @Override
    protected void startScan() {
        search();
    }
}
