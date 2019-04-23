package com.huawei.oss.at.corba.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.huawei.oss.at.corba.exception.CorbaRequestAbnormalException;
import com.huawei.oss.at.corba.model.InvokeParam;
import com.huawei.oss.at.corba.model.InvokeResult;
import com.huawei.oss.at.corba.orb.OrbManager;
import com.huawei.oss.at.corba.orb.OrbUtil;
import com.huawei.oss.at.corba.orb.OrbWrapper;
import com.huawei.oss.at.corba.orb.ParamBuilder;
import org.omg.CORBA.*;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.omg.DynamicAny.DynAnyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.lang.Object;
import java.util.Map;

@Service
public class InterfaceInvokeServiceImpl implements InterfaceInvokeService {

    @Override
    public InvokeResult invokeInterface(InvokeParam invokeParam) {
        String interfaceTypeId = invokeParam.getInterfaceTypeId();
        String namingPath = invokeParam.getInterfaceNamingPath();

        logger.info("begin invoke corba request, naming_path {} interfaceTypeId {}", namingPath, interfaceTypeId);

        OrbWrapper orbWrapper = OrbManager.getInstance().getOrbWrapper(
                OrbUtil.buildOrbConfig(invokeParam.getHostIp()), true);
        Repository ifRepository = orbWrapper.getInterfaceRepository(invokeParam.getIfRespositoryEnv());
        NamingContextExt u2020Naming = orbWrapper.getNamingContext();
        DynAnyFactory dynAnyFactory = orbWrapper.getDynAnyFactory();

        Map<String, Object> paramNameValueMap = invokeParam.getInterfaceParams();
        Contained opContain = ifRepository.lookup_id(interfaceTypeId);
        OperationDef opDef = OperationDefHelper.narrow(opContain);
        if (opDef == null) {
            throw new CorbaRequestAbnormalException(String.format("%s not found in interface repository", interfaceTypeId));
        }
        logger.info("query {} from interface repository completed", interfaceTypeId);

        try {
            String operationName = opDef.name();
            org.omg.CORBA.Object invokeCorbaObject = u2020Naming.resolve_str(namingPath);
            org.omg.CORBA.Request invokeCorbaRequest = invokeCorbaObject._request(opDef.name());

            // set exceptions
            ExceptionDef[] exceptionDefs = opDef.exceptions();
            for (ExceptionDef exceptionDef : exceptionDefs) {
                invokeCorbaRequest.exceptions().add(exceptionDef.type());
            }
            logger.info("set {} exceptions completed", operationName);

            // set params
            ParameterDescription[] paramDescs = opDef.params();
            for (ParameterDescription paramDesc : paramDescs) {
                Any paramVal = ParamBuilder.buildRequestParam(invokeCorbaRequest, paramDesc);
                TypeCode paramType = paramDesc.type;
                if (paramType.kind() == TCKind.tk_alias) {
                    paramType = ParamBuilder.getContentTypeForAlias(paramType);
                }
                Object contractParamVal = paramNameValueMap.get(paramDesc.name);
                org.omg.DynamicAny.DynAny anyParam = ParamBuilder.buildDynAnyParam(dynAnyFactory, paramDesc.name, paramType, contractParamVal);
                paramVal.type(anyParam.type());
                paramVal.read_value(anyParam.to_any().create_input_stream(), anyParam.type());
            }
            logger.info("set {} params completed", operationName);

            // set return type
            invokeCorbaRequest.set_return_type(opDef.result());
            logger.info("set {} return type completed", operationName);

            // dynamic invoke corba request
            invokeCorbaRequest.invoke();
            logger.info("invoke {} completed", operationName);

            // get exception
            UnknownUserException unknownUserException = (UnknownUserException) invokeCorbaRequest.env().exception();
            if (unknownUserException != null) {
                Any anyException = unknownUserException.except;
                org.omg.DynamicAny.DynAny dynAnyException = dynAnyFactory.create_dyn_any(anyException);
                Object exceptionValue = ParamBuilder.buildJSONParm(dynAnyFactory, dynAnyException, dynAnyException.type());
                JSONObject jsonException = new JSONObject();
                jsonException.put("id", anyException.type().id());
                jsonException.put("value", exceptionValue);
                logger.error("invoke {} get unknown user exception: {}", interfaceTypeId, jsonException);
                throw new CorbaRequestAbnormalException(JSON.toJSONString(jsonException));
            }

            // get inout/out arguments
            JSONArray jsonOutputParams = new JSONArray();
            NVList arguments = invokeCorbaRequest.arguments();
            for (int i = 0; i < arguments.count(); i++) {
                NamedValue argument = arguments.item(i);
                switch (argument.flags()) {
                    case ARG_IN.value:
                        break;
                    case ARG_OUT.value:
                    case ARG_INOUT.value:
                        Any anyOutParam = argument.value();
                        org.omg.DynamicAny.DynAny dynAnyOutParam = dynAnyFactory.create_dyn_any(anyOutParam);
                        JSONObject jsonArg = new JSONObject();
                        jsonArg.put("name", argument.name());
                        jsonArg.put("value", ParamBuilder.buildJSONParm(dynAnyFactory, dynAnyOutParam, dynAnyOutParam.type()));
                        jsonOutputParams.add(jsonArg);
                        logger.info("{} param {} value {}", interfaceTypeId, argument.name(), jsonArg);
                        break;
                }
            }
            logger.info("get {} out/inout params completed", operationName);

            // get return
            JSONObject jsonResult = new JSONObject();
            jsonResult.put("name", "result");
            Any anyResult = invokeCorbaRequest.return_value();
            if (anyResult.type().kind() != TCKind.tk_void) {
                org.omg.DynamicAny.DynAny dynAnyResult = dynAnyFactory.create_dyn_any_from_type_code(opDef.result());
                dynAnyResult.from_any(anyResult);
                jsonResult.put("value", ParamBuilder.buildJSONParm(dynAnyFactory, dynAnyResult, dynAnyResult.type()));
                logger.info("{} return {}", operationName, jsonResult);
            } else {
                jsonResult.put("value", "void");
            }
            logger.info("get {} return completed", operationName);

            InvokeResult result = new InvokeResult();
            result.setStatus(HttpStatus.OK);
            result.setOutParams(jsonOutputParams);
            result.setResult(jsonResult);

            logger.info("invoke {} successfully", operationName);

            return result;
        } catch (CorbaRequestAbnormalException ex) {
            throw ex;
        } catch (NotFound ex) {
            logger.error("corba request invoke catch naming not found exception", ex);
            throw new CorbaRequestAbnormalException(String.format("%s not found from naming", namingPath));
        } catch (Exception ex) {
            logger.error("corba request invoke catch exception", ex);
            throw new CorbaRequestAbnormalException(String.format("catch unkown exceptioin: %s", ex.getMessage()));
        }
    }

    private static Logger logger = LoggerFactory.getLogger(InterfaceInvokeServiceImpl.class);
}
