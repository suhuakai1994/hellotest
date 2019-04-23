package com.huawei.oss.at.corba.orb;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.huawei.oss.at.corba.common.Utils;
import com.huawei.oss.at.corba.exception.CorbaRequestAbnormalException;
import org.omg.CORBA.*;
import org.omg.CORBA.TypeCodePackage.BadKind;
import org.omg.CORBA.TypeCodePackage.Bounds;
import org.omg.DynamicAny.DynAny;
import org.omg.DynamicAny.*;
import org.omg.DynamicAny.DynAnyFactoryPackage.InconsistentTypeCode;
import org.omg.DynamicAny.DynAnyPackage.InvalidValue;
import org.omg.DynamicAny.DynAnyPackage.TypeMismatch;
import org.omg.DynamicAny.DynEnum;
import org.omg.DynamicAny.DynSequence;
import org.omg.DynamicAny.DynStruct;
import org.omg.DynamicAny.DynUnion;
import org.omg.DynamicAny.NameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Object;

public class ParamBuilder {

    public static TypeCode getContentTypeForAlias(TypeCode aliasTypeCode) throws BadKind {
        TypeCode contentTypeCode = aliasTypeCode.content_type();

        while (contentTypeCode.kind() == TCKind.tk_alias) {
            contentTypeCode = contentTypeCode.content_type();
        }

        return contentTypeCode;
    }

    public static Any buildRequestParam(Request request, ParameterDescription paramDesc) {
        org.omg.CORBA.Any paramAny = null;

        switch (paramDesc.mode.value()) {
            case ParameterMode._PARAM_IN:
                paramAny = request.add_named_in_arg(paramDesc.name);
                break;
            case ParameterMode._PARAM_OUT:
                paramAny = request.add_named_out_arg(paramDesc.name);
                break;
            case ParameterMode._PARAM_INOUT:
                paramAny = request.add_named_inout_arg(paramDesc.name);
                break;
        }

        return paramAny;
    }

    public static DynAny buildDynAnyParam(DynAnyFactory dynAnyFactory, String paramName, TypeCode paramType, Object contractParamVal) {
        try {
            DynAny paramAny = dynAnyFactory.create_dyn_any_from_type_code(paramType);
            logger.trace("build param [{}] type [{}] value [{}]", paramName, paramType.kind(), contractParamVal);

            if (contractParamVal == null) {
                return paramAny;
            }

            String contractParamValStr = contractParamVal.toString();
            int paramTypeKind = paramType.kind().value();
            switch (paramTypeKind) {
                case TCKind._tk_short:
                    paramAny.insert_short(Short.parseShort(contractParamValStr));
                    break;
                case TCKind._tk_long:
                    paramAny.insert_long(Integer.parseInt(contractParamValStr));
                    break;
                case TCKind._tk_ushort:
                    paramAny.insert_ushort(Short.parseShort(contractParamValStr));
                    break;
                case TCKind._tk_ulong:
                    paramAny.insert_ulong(Integer.parseUnsignedInt(contractParamValStr));
                    break;
                case TCKind._tk_float:
                    paramAny.insert_float(Float.parseFloat(contractParamValStr));
                    break;
                case TCKind._tk_double:
                case TCKind._tk_longdouble:
                    paramAny.insert_double(Double.parseDouble(contractParamValStr));
                    break;
                case TCKind._tk_boolean:
                    paramAny.insert_boolean(Boolean.parseBoolean(contractParamValStr));
                    break;
                case TCKind._tk_char:
                    paramAny.insert_char(contractParamValStr.charAt(0));
                    break;
                case TCKind._tk_octet:
                    paramAny.insert_octet(Byte.parseByte(contractParamValStr));
                    break;
                case TCKind._tk_struct:
                    JSONObject contractParamObject = (JSONObject) contractParamVal;
                    DynStruct paramStruct = (DynStruct) paramAny;
                    int memberCount = paramType.member_count();
                    NameValuePair[] memberValues = new NameValuePair[memberCount];
                    for (int i = 0; i < memberCount; i++) {
                        String memberName = paramType.member_name(i);
                        TypeCode memberType = paramType.member_type(i);
                        Object memberJsonValue = contractParamObject.get(memberName);
                        DynAny memberAny = buildDynAnyParam(dynAnyFactory, memberName, memberType, memberJsonValue);
                        memberValues[i] = new NameValuePair(memberName, memberAny.to_any());
                    }
                    paramStruct.set_members(memberValues);
                    break;
                case TCKind._tk_union:
                    logger.error("DynUnion not support");
                    break;
                case TCKind._tk_enum:
                    if (contractParamValStr.equals("")) {
                        break;
                    }
                    DynEnum dynEnum = (DynEnum) paramAny;
                    try {
                        // try to set enum with string value
                        dynEnum.set_as_string(contractParamValStr);
                    } catch (InvalidValue invalidValue) {
                        Integer intValue = Utils.tryParseInt(contractParamValStr);
                        if (intValue != null) {
                            // try to set enum with int value
                            dynEnum.set_as_ulong(intValue.intValue());
                        } else {
                            throw invalidValue;
                        }
                    }
                    break;
                case TCKind._tk_string:
                    paramAny.insert_string(contractParamValStr);
                    break;
                case TCKind._tk_sequence:
                    JSONArray contractParamArray = JSONArray.parseArray(contractParamValStr);
                    int sequenceLength = contractParamArray.size();
                    int dynSequenceLength = sequenceLength > 0 ? sequenceLength : 1;
                    DynAny[] itemValues = new DynAny[dynSequenceLength];
                    TypeCode itemType = paramType.content_type();
                    DynSequence paramSequence = (DynSequence) paramAny;
                    paramSequence.set_length(dynSequenceLength);
                    for (int i = 0; i < dynSequenceLength; i++) {
                        Object itemValue = null;
                        if (sequenceLength > 0) {
                            itemValue = contractParamArray.get(i);
                        }
                        itemValues[i] = buildDynAnyParam(dynAnyFactory, "", itemType, itemValue);
                    }
                    paramSequence.set_elements_as_dyn_any(itemValues);
                    break;
                case TCKind._tk_longlong:
                    paramAny.insert_longlong(Long.parseLong(contractParamValStr));
                    break;
                case TCKind._tk_ulonglong:
                    paramAny.insert_ulonglong(Long.parseLong(contractParamValStr));
                    break;
                case TCKind._tk_wchar:
                    paramAny.insert_wchar(contractParamValStr.charAt(0));
                    break;
                case TCKind._tk_wstring:
                    paramAny.insert_wstring(contractParamValStr);
                    break;
                case TCKind._tk_alias:
                    paramType = getContentTypeForAlias(paramType);
                    paramAny = buildDynAnyParam(dynAnyFactory, paramName, paramType, contractParamVal);
                    break;
                default:
                    logger.error("not support corba type {}", paramType.kind().toString());
                    break;
            }
            return paramAny;
        } catch (CorbaRequestAbnormalException cex) {
            throw cex;
        } catch (InvalidValue invalidValue) {
            logger.error("build param catch exception invalidvalue", invalidValue);
            throw new CorbaRequestAbnormalException(invalidValue.getMessage());
        } catch (TypeMismatch typeMismatch) {
            logger.error("build param catch exception typemismatch", typeMismatch);
            throw new CorbaRequestAbnormalException(typeMismatch.getMessage());
        } catch (BadKind badKind) {
            logger.error("build param catch exception badkind", badKind);
            throw new CorbaRequestAbnormalException(badKind.getMessage());
        } catch (Bounds bounds) {
            logger.error("build param catch exception bounds", bounds);
            throw new CorbaRequestAbnormalException(bounds.getMessage());
        } catch (InconsistentTypeCode inconsistentTypeCode) {
            logger.error("build param catch exception inconsistenttypecode", inconsistentTypeCode);
            throw new CorbaRequestAbnormalException(inconsistentTypeCode.getMessage());
        } catch (Exception ex) {
            logger.error("build param catch exception", ex);
            throw new CorbaRequestAbnormalException(ex.getMessage());
        }
    }

    public static Object buildJSONParm(DynAnyFactory dynAnyFactory, DynAny paramAny, TypeCode paramType) {
        try {
            Object param = null;
            switch (paramType.kind().value()) {
                case TCKind._tk_short:
                    param = paramAny.get_short();
                    break;
                case TCKind._tk_long:
                    param = paramAny.get_long();
                    break;
                case TCKind._tk_ushort:
                    param = paramAny.get_ushort();
                    break;
                case TCKind._tk_ulong:
                    param = paramAny.get_ulong();
                    break;
                case TCKind._tk_float:
                    param = paramAny.get_float();
                    break;
                case TCKind._tk_double:
                case TCKind._tk_longdouble:
                    param = paramAny.get_double();
                    break;
                case TCKind._tk_boolean:
                    param = paramAny.get_boolean();
                    break;
                case TCKind._tk_char:
                    param = String.valueOf(paramAny.get_char());
                    break;
                case TCKind._tk_octet:
                    param = paramAny.get_octet();
                    break;
                case TCKind._tk_except:
                case TCKind._tk_struct:
                    JSONObject paramObject = new JSONObject();
                    DynStruct paramStruct = (DynStruct) paramAny;
                    NameValuePair[] members = paramStruct.get_members();
                    for (int i = 0; i < members.length; i++) {
                        try {
                            DynAny memberAny = dynAnyFactory.create_dyn_any(members[i].value);
                            paramObject.put(members[i].id, buildJSONParm(dynAnyFactory, memberAny, memberAny.type()));
                        } catch (InconsistentTypeCode inconsistentTypeCode) {
                            logger.error("build jsonparam catch exception inconsistenttypecode", inconsistentTypeCode);
                        } catch (Exception ex) {
                            logger.error("build jsonparam struct member catch exception", ex);
                            paramObject.put(members[i].id, null);
                        }
                    }
                    param = paramObject;
                    break;
                case TCKind._tk_union:
                    DynUnion paramUnion = (DynUnion) paramAny;
                    logger.error("DynUnion not support");
                    break;
                case TCKind._tk_enum:
                    DynEnum dynEnum = (DynEnum) paramAny;
                    param = dynEnum.get_as_string();
                    break;
                case TCKind._tk_string:
                    param = paramAny.get_string();
                    break;
                case TCKind._tk_sequence:
                    JSONArray paramArrayForSeq = new JSONArray();
                    DynSequence paramSequence = (DynSequence) paramAny;
                    DynAny[] elements = paramSequence.get_elements_as_dyn_any();
                    for (int i = 0; i < elements.length; i++) {
                        paramArrayForSeq.add(i, buildJSONParm(dynAnyFactory, elements[i], elements[i].type()));
                    }
                    param = paramArrayForSeq;
                    break;
                case TCKind._tk_longlong:
                    param = paramAny.get_longlong();
                    break;
                case TCKind._tk_ulonglong:
                    param = paramAny.get_ulonglong();
                    break;
                case TCKind._tk_wchar:
                    param = String.valueOf(paramAny.get_wchar());
                    break;
                case TCKind._tk_wstring:
                    param = paramAny.get_wstring();
                    break;
                case TCKind._tk_alias:
                    paramType = getContentTypeForAlias(paramType);
                    param = buildJSONParm(dynAnyFactory, paramAny, paramType);
                    break;
                default:
                    logger.error("not support corba type {}", paramType.kind().toString());
                    break;
            }
            return param;
        } catch (CorbaRequestAbnormalException cex) {
            throw cex;
        } catch (InvalidValue invalidValue) {
            logger.error("build jsonparam catch exception invalidvalue", invalidValue);
            throw new CorbaRequestAbnormalException(invalidValue.getMessage());
        } catch (TypeMismatch typeMismatch) {
            logger.error("build jsonparam catch exception typemismatch", typeMismatch);
            throw new CorbaRequestAbnormalException(typeMismatch.getMessage());
        } catch (BadKind badKind) {
            logger.error("build jsonparam catch exception badkind", badKind);
            throw new CorbaRequestAbnormalException(badKind.getMessage());
        } catch (Exception ex) {
            logger.error("build jsonparam catch exception", ex);
            throw new CorbaRequestAbnormalException(ex.getMessage());
        }
    }

    private static Logger logger = LoggerFactory.getLogger(ParamBuilder.class);
}
