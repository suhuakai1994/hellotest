package com.huawei.oss.at.corba.service;

import com.huawei.oss.at.corba.model.InvokeParam;
import com.huawei.oss.at.corba.model.InvokeResult;

public interface InterfaceInvokeService {

    InvokeResult invokeInterface(InvokeParam invokeParam);
}
