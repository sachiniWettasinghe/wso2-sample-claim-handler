package org.wso2.custom.claim.internal;

import org.wso2.carbon.user.core.service.RealmService;

/**
 * DataHolder class for the custom claim handler.
 */
public class CustomClaimHandlerDataHolder {

    private static CustomClaimHandlerDataHolder customClaimHandlerDataHolder = new CustomClaimHandlerDataHolder();

    private RealmService realmService;

    private CustomClaimHandlerDataHolder() {

    }

    public static CustomClaimHandlerDataHolder getInstance() {

        return customClaimHandlerDataHolder;
    }

    public RealmService getRealmService() {

        return realmService;
    }

    public void setRealmService(RealmService realmService) {

        this.realmService = realmService;
    }
}
