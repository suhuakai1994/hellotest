package com.huawei.oss.at.corba.orb;

import com.alibaba.fastjson.JSONArray;
import com.huawei.oss.at.corba.common.Constant;
import com.huawei.oss.at.corba.common.Utils;
import com.huawei.oss.at.corba.exception.CorbaRequestAbnormalException;
import com.huawei.oss.at.corba.exception.TokenAbnormalException;
import com.huawei.oss.at.corba.model.InvokeParam;
import com.huawei.oss.at.corba.session.LoginConfig;
import com.huawei.oss.at.corba.session.SessionManager;
import org.omg.CORBA.*;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.omg.DynamicAny.DynAny;
import org.omg.DynamicAny.DynAnyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenUtil {

    public static JSONArray getInterfaceInvokeToken(InvokeParam invokeParam, String u2020Version) {
        logger.info("get intreface invoke token by version {}", u2020Version);
        JSONArray sessionArray;
        String session = SessionManager.getInstance().getUserSession(
                LoginConfig.buildLoginConfig(invokeParam.getHostIp(), invokeParam.getUserName(), invokeParam.getUserPwd()));
        switch (u2020Version) {
            case "iManagerU2020V300R019C00":
                logger.info("get interface invoke token match version {}", u2020Version);
                sessionArray = getTokenByBspSession(invokeParam.getHostIp(), session, invokeParam.getIfRespositoryEnv());
                break;
            default:
                logger.info("get interface invoke token match default");
                sessionArray = (JSONArray) JSONArray.toJSON(Utils.getUserToken(session));
                break;
        }
        return sessionArray;
    }

    private static JSONArray getTokenByBspSession(String hostIp, String bspSession, String ifrRepositoryEnv) {
        try {
            OrbWrapper orbWrapper = OrbManager.getInstance().getOrbWrapper(OrbUtil.buildOrbConfig(hostIp), true);
            Repository ifRepository = orbWrapper.getInterfaceRepository(ifrRepositoryEnv);
            NamingContextExt u2020Naming = orbWrapper.getNamingContext();
            DynAnyFactory dynAnyFactory = orbWrapper.getDynAnyFactory();
            org.omg.CORBA.Object sessionMgr = u2020Naming.resolve_str(Constant.TOKEN_NAMING_PATH);
            org.omg.CORBA.Request request = sessionMgr._request(Constant.TOKEN_OPERATION);
            Contained opContain = ifRepository.lookup_id(Constant.TOKEN_IDL_PATH);
            OperationDef opDef = OperationDefHelper.narrow(opContain);
            if (opDef == null) {
                throw new CorbaRequestAbnormalException(String.format("%s not found in interface repository", Constant.TOKEN_IDL_PATH));
            }
            ParameterDescription[] paramDescs = opDef.params();
            for (ParameterDescription paramDesc : paramDescs) {
                org.omg.CORBA.Any paramVal = request.add_named_in_arg("bspsession");
                DynAny anyParam = ParamBuilder.buildDynAnyParam(dynAnyFactory, paramDesc.name, paramDesc.type, bspSession);
                paramVal.type(anyParam.type());
                paramVal.read_value(anyParam.to_any().create_input_stream(), anyParam.type());
            }
            request.set_return_type(opDef.result());
            request.invoke();
            Any anyResult = request.return_value();
            DynAny dynAnyResult = dynAnyFactory.create_dyn_any_from_type_code(opDef.result());
            dynAnyResult.from_any(anyResult);
            JSONArray token = (JSONArray) (ParamBuilder.buildJSONParm(dynAnyFactory, dynAnyResult, dynAnyResult.type()));
            return token;
        } catch (CorbaRequestAbnormalException ex) {
            throw ex;
        } catch (NotFound notFound) {
            logger.error("getTokenByBspSession catch exception", notFound);
            throw new TokenAbnormalException("get token failed, check getTokenByBspsession existence in naming");
        } catch (Exception ex) {
            logger.error("getTokenByBspSession catch exception", ex);
            throw new TokenAbnormalException("get token failed, maybe execute agent's ip is not in env white ip list");
        }
    }

    private static Logger logger = LoggerFactory.getLogger(TokenUtil.class);
}
