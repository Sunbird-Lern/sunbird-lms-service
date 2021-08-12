package org.sunbird.dao.role.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.dao.role.RoleGroupDao;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.role.RoleGroup;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

public class RoleGroupDaoImpl implements RoleGroupDao {

    private ObjectMapper mapper = new ObjectMapper();
    private static RoleGroupDao roleGroupDao;
    private static final String KEYSPACE_NAME = "sunbird";
    private static final String TABLE_NAME = "role_group";

    public static RoleGroupDao getInstance() {
        if (roleGroupDao == null) {
            roleGroupDao = new RoleGroupDaoImpl();
        }
        return roleGroupDao;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RoleGroup> getRoleGroups(RequestContext context) {
        Response roleGroupResults =
                getCassandraOperation().getAllRecords(KEYSPACE_NAME, TABLE_NAME, context);
        TypeReference<List<RoleGroup>> roleGroupType = new TypeReference<List<RoleGroup>>() {};
        List<Map<String, Object>> roleGroupMapList =
                (List<Map<String, Object>>) roleGroupResults.get(JsonKey.RESPONSE);
        List<RoleGroup> roleGroupList = mapper.convertValue(roleGroupMapList, roleGroupType);
        return roleGroupList;
    }

    public CassandraOperation getCassandraOperation() {
        return ServiceFactory.getInstance();
    }
}
