package com.huawei.oss.at.corba.service;

import com.huawei.oss.at.corba.exception.CorbaRequestAbnormalException;
import com.huawei.oss.at.corba.model.QueryInterfaceParam;
import com.huawei.oss.at.corba.model.QueryInterfaceResult;
import com.huawei.oss.at.corba.model.QueryParamResult;
import com.huawei.oss.at.corba.orb.OrbManager;
import com.huawei.oss.at.corba.orb.OrbUtil;
import com.huawei.oss.at.corba.orb.OrbWrapper;
import com.huawei.oss.at.corba.orb.ParamBuilder;
import com.huawei.oss.at.corba.repository.RepositoryManager;
import org.apache.commons.lang3.StringUtils;
import org.omg.CORBA.*;
import org.omg.CosNaming.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class InterfaceQueryServiceImpl implements InterfaceQueryService {

    public InterfaceQueryServiceImpl() {
        paramTypeMap = new HashMap<>();
        paramTypeMap.put(ParameterMode._PARAM_IN, "in");
        paramTypeMap.put(ParameterMode._PARAM_OUT, "out");
        paramTypeMap.put(ParameterMode._PARAM_INOUT, "inout");
    }

    @Override
    public List<QueryParamResult> queryInterfaceParams(String interfaceTypeId, String ifRepositoryEnv) {
        try {
            logger.info("query interface params interfaceTypeId {}", interfaceTypeId);
            List<QueryParamResult> listQueryParamResults = new ArrayList<>();
            Repository ifRepository = RepositoryManager.getInstance().getInterfaceRepository(ifRepositoryEnv);
            Contained opContain = ifRepository.lookup_id(interfaceTypeId);
            OperationDef opDef = OperationDefHelper.narrow(opContain);
            ParameterDescription[] paramDescs = opDef.params();

            for (ParameterDescription paramDesc : paramDescs) {
                TypeCode paramType = paramDesc.type;
                if (paramType.kind() == TCKind.tk_alias) {
                    paramType = ParamBuilder.getContentTypeForAlias(paramType);
                }
                QueryParamResult queryParamResult = new QueryParamResult();
                queryParamResult.setParamName(paramDesc.name);
                queryParamResult.setParamValue("");
                queryParamResult.setParamKind(paramType.kind().value());
                queryParamResult.setParamType(paramTypeMap.get(paramDesc.mode.value()));
                listQueryParamResults.add(queryParamResult);
            }

            logger.info("query interface params interfaceTypeId {} successfully", interfaceTypeId);
            return listQueryParamResults;
        } catch (Exception ex) {
            logger.error("query interface param catch exception", ex);
            throw new CorbaRequestAbnormalException("query interface param failed");
        }
    }

    @Override
    public List<QueryInterfaceResult> queryInterfaces(QueryInterfaceParam queryInterfaceParam) {
        try {
            logger.info("query interfaces {}", queryInterfaceParam);
            List<QueryInterfaceResult> listQueryInterfaceResults = new ArrayList<>();
            OrbWrapper orbWrapper = OrbManager.getInstance().getOrbWrapper(
                    OrbUtil.buildOrbConfig(queryInterfaceParam.getHostIp()), true);
            Repository ifRepository = orbWrapper.getInterfaceRepository(queryInterfaceParam.getInterfaceRepositoryEnv());
            NamingContextExt u2020Naming = orbWrapper.getNamingContext();

            org.omg.CORBA.Object testCorbaObj = u2020Naming.resolve_str(queryInterfaceParam.getCorbaObjectNamingPath());
            org.openorb.orb.core.ObjectStub stub = (org.openorb.orb.core.ObjectStub) testCorbaObj;
            org.openorb.orb.core.Delegate delegate = (org.openorb.orb.core.Delegate) stub._get_delegate();
            String interfaceTypeId = delegate.ior().type_id;
            Contained opContain = ifRepository.lookup_id(interfaceTypeId);
            InterfaceDef interfaceDef = InterfaceDefHelper.narrow(opContain);
            OperationDescription[] operationDescriptions = interfaceDef.describe_interface().operations;

            for (OperationDescription operDesc : operationDescriptions) {
                QueryInterfaceResult queryInterfaceResult = new QueryInterfaceResult();
                queryInterfaceResult.setInterfaceName(operDesc.name);
                queryInterfaceResult.setInterfaceType(operDesc.id);
                listQueryInterfaceResults.add(queryInterfaceResult);
            }

            logger.info("query interfaces {} successfully", queryInterfaceParam);
            return listQueryInterfaceResults;
        } catch (Exception ex) {
            logger.error("query interface catch exception", ex);
            throw new CorbaRequestAbnormalException("query interface failed");
        }
    }

    @Override
    public List<String> queryObjects(String hostIp, String baseNamingPath) {
        try {
            logger.info("query objects {} {}", hostIp, baseNamingPath);
            List<String> corbaObjects = new ArrayList<>();
            OrbWrapper orbWrapper = OrbManager.getInstance().getOrbWrapper(
                    OrbUtil.buildOrbConfig(hostIp), true);
            NamingContextExt u2020Naming = orbWrapper.getNamingContext();
            NamingContext namingObject = u2020Naming;
            baseNamingPath = StringUtils.stripEnd(baseNamingPath, "/");
            if (StringUtils.isNotBlank(baseNamingPath)) {
                namingObject = NamingContextHelper.narrow(u2020Naming.resolve_str(baseNamingPath));
                baseNamingPath = baseNamingPath + "/";
            } else {
                // if baseNamingPath is null or bank, set to empty string
                baseNamingPath = "";
            }

            BindingListHolder bindingListHolder = new BindingListHolder();
            BindingIteratorHolder bindingIteratorHolder = new BindingIteratorHolder();

            namingObject.list(OBJECT_QUERY_NUM, bindingListHolder, bindingIteratorHolder);
            for (Binding binding : bindingListHolder.value) {
                StringBuilder namingPathBuilder = new StringBuilder();
                namingPathBuilder.append(baseNamingPath);
                namingPathBuilder.append(String.join("/",
                        Arrays.stream(binding.binding_name).map(n -> n.id).collect(Collectors.toList())));
                corbaObjects.add(namingPathBuilder.toString());
            }
            corbaObjects.sort(String.CASE_INSENSITIVE_ORDER);

            logger.info("query objects {} {} successfully", hostIp, baseNamingPath);
            return corbaObjects;
        } catch (Exception ex) {
            logger.error("query objects catch exception", ex);
            throw new CorbaRequestAbnormalException("query objects failed");
        }
    }

    private Map<Integer, String> paramTypeMap;

    private static final int OBJECT_QUERY_NUM = 100;

    private static Logger logger = LoggerFactory.getLogger(InterfaceQueryServiceImpl.class);
}
