package org.wso2.custom.claim;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.config.model.ApplicationConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.model.StepConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.handler.claims.impl.DefaultClaimHandler;
import org.wso2.carbon.identity.claim.metadata.mgt.ClaimMetadataHandler;
import org.wso2.carbon.identity.claim.metadata.mgt.exception.ClaimMetadataException;
import org.wso2.carbon.identity.scim2.common.exceptions.IdentitySCIMException;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.charon3.core.exceptions.CharonException;
import org.wso2.custom.claim.dao.CustomClaimHandlerDAO;
import org.wso2.custom.claim.internal.CustomClaimHandlerDataHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Custom claim handler to return groupid in oidc id_token.
 */
public class CustomClaimHandler extends DefaultClaimHandler {

    private static Log log = LogFactory.getLog(CustomClaimHandler.class);

    @Override
    protected Map<String, String> handleLocalClaims(String spStandardDialect, StepConfig stepConfig,
                                                    AuthenticationContext context) throws FrameworkException {

        Map<String, String> localClaims = super.handleLocalClaims(spStandardDialect, stepConfig, context);

        if (localClaims == null || localClaims.isEmpty()) {
            return localClaims;
        }

        // Return if the groupid is not requested.
        Map<String, String> requestedClaimMappings = getRequestedClaimMappings(context);
        if (!requestedClaimMappings.containsValue("http://wso2.org/claims/groupid")) {
            return localClaims;
        }

        String roleClaimValue = getRoleClaimValue(spStandardDialect, context, localClaims);
        String groupIdClaimName = getGroupIdClaimName(spStandardDialect, context);

        if (StringUtils.isBlank(roleClaimValue)) {
            return localClaims;
        }

        boolean isSCIMEnabled;
        try {
            isSCIMEnabled = isSCIMEnabled(context.getLastAuthenticatedUser().getUserStoreDomain(),
                    getUserManager(context.getTenantDomain()));
        } catch (CharonException e) {
            throw new FrameworkException("Error while checking SCIM enabled for the user store.", e);
        }

        if (!isSCIMEnabled) {
            return localClaims;
        }

        String attributeSeparator = ",";
        if (StringUtils.isNotBlank(localClaims.get("MultiAttributeSeparator"))) {
            attributeSeparator = localClaims.get("MultiAttributeSeparator");
        }

        List<String> rolesList = Arrays.asList(roleClaimValue.split(attributeSeparator));
        List<String> refinedRolesList = getRefinedRolesList(context, rolesList);

        localClaims = addGroupIdClaim(context, refinedRolesList, groupIdClaimName, localClaims, attributeSeparator);

        return localClaims;
    }

    private Map<String, String> addGroupIdClaim(AuthenticationContext context, List<String> refinedRolesList,
                                                String groupIdClaimName, Map<String, String> localClaims,
                                                String attributeSeparator) throws FrameworkException {

        try {
            Map<String, String> groupNameIdMap = CustomClaimHandlerDAO.getInstance()
                    .getGroupIds(refinedRolesList, getTenantId(context.getTenantDomain()));
            if (groupNameIdMap.isEmpty()) {
                return localClaims;
            }

            List<String> groupId = new ArrayList<>();
            for (Map.Entry<String, String> entry : groupNameIdMap.entrySet()) {
                groupId.add(entry.getKey() + "=" + entry.getValue());
            }

            localClaims.put(groupIdClaimName, StringUtils.join(groupId, attributeSeparator));
        } catch (IdentitySCIMException e) {
            throw new FrameworkException("Error in getting groupids.", e);
        } catch (UserStoreException e) {
            throw new FrameworkException("Error in getting tenant id.", e);
        }

        return localClaims;
    }

    private UserStoreManager getUserManager(String tenantDomain) throws CharonException {

        UserStoreManager userStoreManager = null;
        try {
            RealmService realmService = CustomClaimHandlerDataHolder.getInstance().getRealmService();
            if (realmService != null) {
                int tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
                UserRealm userRealm = realmService.getTenantUserRealm(tenantId);
                if (userRealm != null) {
                    userStoreManager = (UserStoreManager) userRealm.getUserStoreManager();
                }
            } else {
                throw new CharonException("Can not obtain carbon realm service.");
            }
        } catch (UserStoreException e) {
            throw new CharonException("Error obtaining user realm for tenant: " + tenantDomain, e);
        }
        return userStoreManager;
    }

    private boolean isSCIMEnabled(String userStoreName, UserStoreManager userStoreManager) {

        userStoreManager = userStoreManager.getSecondaryUserStoreManager(userStoreName);
        if (userStoreManager != null) {
            try {
                return userStoreManager.isSCIMEnabled();
            } catch (UserStoreException e) {
                log.error("Error while evaluating isSCIMEnalbed for user store " + userStoreName, e);
            }
        }
        return false;
    }

    private int getTenantId(String tenantDomain) throws UserStoreException {

        RealmService realmService = CustomClaimHandlerDataHolder.getInstance().getRealmService();
        int tenantId = realmService.getTenantManager().getTenantId(tenantDomain);

        return tenantId;
    }

    private Map<String, String> getRequestedClaimMappings(AuthenticationContext context) {

        ApplicationConfig appConfig = context.getSequenceConfig().getApplicationConfig();
        Map<String, String> requestedClaimMappings = appConfig.getRequestedClaimMappings();

        return requestedClaimMappings;
    }

    private String getRoleClaimValue(String spStandardDialect, AuthenticationContext context,
                                     Map<String, String> localClaims) throws FrameworkException {

        String roleClaimValue = null;
        if (StringUtils.isNotBlank(spStandardDialect) && !StringUtils.equals(spStandardDialect,
                "http://wso2.org/claims")) {
            Map<String, String> externalClaimMappings = getExternalClaimMappings(spStandardDialect, context);
            for (Map.Entry<String, String> entry : externalClaimMappings.entrySet()) {
                if ("http://wso2.org/claims/role".equals(entry.getValue())) {
                    roleClaimValue = localClaims.get(entry.getKey());
                }
            }
        } else {
            roleClaimValue = localClaims.get("http://wso2.org/claims/role");
        }

        return roleClaimValue;
    }

    private String getGroupIdClaimName(String spStandardDialect, AuthenticationContext context)
            throws FrameworkException {

        String groupIdClaimName = "http://wso2.org/claims/groupid";
        if (StringUtils.isNotBlank(spStandardDialect) && !StringUtils.equals(spStandardDialect,
                "http://wso2.org/claims")) {
            Map<String, String> externalClaimMappings = getExternalClaimMappings(spStandardDialect, context);

            for (Map.Entry<String, String> entry : externalClaimMappings.entrySet()) {
                if ("http://wso2.org/claims/groupid".equals(entry.getValue())) {
                    groupIdClaimName = entry.getKey();
                }
            }
        }

        return groupIdClaimName;
    }

    private Map<String, String> getExternalClaimMappings(String spStandardDialect, AuthenticationContext context)
            throws FrameworkException {

        Map<String, String> externalClaimMappings;
        try {
            externalClaimMappings =
                    ClaimMetadataHandler.getInstance().getMappingsMapFromOtherDialectToCarbon(spStandardDialect,
                            null, context.getTenantDomain(), false);
        } catch (ClaimMetadataException e) {
            throw new FrameworkException("Failed to load sp dialect:" + spStandardDialect, e);
        }

        return externalClaimMappings;
    }

    private List<String> getRefinedRolesList(AuthenticationContext context, List<String> rolesList) {

        List<String> refinedRolesList = new ArrayList<>();

        if ("PRIMARY".equals(context.getLastAuthenticatedUser().getUserStoreDomain())) {
            for (String role : rolesList) {
                if (!role.contains("/")) {
                    refinedRolesList.add("PRIMARY/" + role);
                } else {
                    refinedRolesList.add(role);
                }
            }
        } else {
            refinedRolesList = rolesList;
        }

        return refinedRolesList;
    }

}
