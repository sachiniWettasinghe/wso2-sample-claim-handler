package org.wso2.custom.claim.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.user.core.service.RealmService;

@Component(
        name = "sample.claim.handler",
        immediate = true
)
public class CustomClaimHandlerServiceComponent {

    private static Log log = LogFactory.getLog(CustomClaimHandlerServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) {

        try {
            log.info("Carbon Custom Claim Handler activated successfully.");
        } catch (Exception e) {
            log.error("Failed to activate Carbon Custom Claim Handler ", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        if (log.isDebugEnabled()) {
            log.debug("Carbon Custom Claim Handler is deactivated.");
        }
    }

    @Reference(
            name = "user.realmservice.default",
            service = RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService"
    )
    protected void setRealmService(RealmService realmService) {

        if (log.isDebugEnabled()) {
            log.debug("RealmService is set in the Carbon Custom Claim Handler bundle.");
        }
        CustomClaimHandlerDataHolder.getInstance().setRealmService(realmService);

    }

    protected void unsetRealmService(RealmService realmService) {

        if (log.isDebugEnabled()) {
            log.debug("RealmService is unset in the Carbon Custom Claim Handler bundle.");
        }
        CustomClaimHandlerDataHolder.getInstance().setRealmService(null);

    }

}
