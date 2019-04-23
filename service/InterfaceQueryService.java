package com.huawei.oss.at.corba.service;

import com.huawei.oss.at.corba.model.QueryInterfaceParam;
import com.huawei.oss.at.corba.model.QueryInterfaceResult;
import com.huawei.oss.at.corba.model.QueryParamResult;

import java.util.List;

public interface InterfaceQueryService {

    List<QueryInterfaceResult> queryInterfaces(QueryInterfaceParam queryInterfaceParam);

    List<QueryParamResult> queryInterfaceParams(String interfaceTypeId, String ifRepositoryEnv);

    List<String> queryObjects(String hostIp, String namingPath);
}
