package org.wso2.custom.claim.dao;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.scim2.common.exceptions.IdentitySCIMException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO class for the custom claim handler.
 */
public class CustomClaimHandlerDAO {

    private static CustomClaimHandlerDAO instance = new CustomClaimHandlerDAO();
    private static final String SELECT_GROUP = "SELECT ROLE_NAME, ATTR_VALUE FROM IDN_SCIM_GROUP WHERE TENANT_ID = ? AND ATTR_NAME = 'urn:scim:schemas:core:1.0:id' AND ROLE_NAME IN (";

    private CustomClaimHandlerDAO() {

    }

    public static CustomClaimHandlerDAO getInstance() {

        return instance;
    }

    public Map<String, String> getGroupIds(List<String> groupNames, int tenantId) throws IdentitySCIMException {

        Map<String, String> groupNameIdMap = new HashMap<>();
        String searchQuery = SELECT_GROUP + "'" + StringUtils.join(groupNames, "','") + "'" + ")";

        try (Connection connection = IdentityDatabaseUtil.getDBConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(searchQuery)) {

            preparedStatement.setInt(1, tenantId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {

                while (resultSet.next()) {
                    groupNameIdMap.put(resultSet.getString("ROLE_NAME"),
                            resultSet.getString("ATTR_VALUE"));
                }
            }
        } catch (SQLException e) {
            throw new IdentitySCIMException("Error when reading groupid from the persistence store.", e);
        }
        return groupNameIdMap;
    }

}
