package com.huawei.oss.at.corba.orb;

import com.huawei.oss.at.corba.exception.CorbaRequestAbnormalException;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.IIOP.ProfileBody_1_1;
import org.omg.IIOP.ProfileBody_1_1Helper;
import org.omg.IIOP.Version;
import org.omg.IOP.*;
import org.omg.IOP.CodecFactoryPackage.UnknownEncoding;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.openorb.orb.core.ObjectStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

public class OrbUtil {
    public static String[] getORBInitArguments(boolean isClientMode, boolean isSSLMode) {
        HashMap<String, String> args = getFixedArguments();
        if (isClientMode) {
            args.put("-ORBProfile", "client-only");
        } else {
            args.put("-ORBProfile", "default");
        }
        return map2array(args);
    }

    public static Properties getORBInitProperties(boolean isClientMode, boolean isSSLMode) {
        HashMap<String, String> props = getFixedProperties();
        if (isSSLMode) {
            //String openORBPath = System.getProperty("openorb.home.path");
            props.put("ImportModule.ssliop", "${openorb.home}config/SSLIOP.xml#ssliop");
            // props.put("ImportModule.ssliop", openORBPath+"/config/SSLIOP.xml#ssliop");
        }
        return map2Properties(props);
    }

    private static HashMap<String, String> getFixedArguments() {
        HashMap<String, String> args = new HashMap();
        String openORBPath = System.getProperty("openorb.home.path");
        String openORBConfigPath = Paths.get(openORBPath, "config", "OpenORB.xml").toString();
        args.put("-ORBOpenORB", openORBPath);
        args.put("-ORBConfig", openORBConfigPath);
        return args;
    }

    private static HashMap<String, String> getFixedProperties() {
        HashMap<String, String> props = new HashMap();
        props.put("org.omg.CORBA.ORBClass", "org.openorb.orb.core.ORB");
        props.put("org.omg.CORBA.ORBSingletonClass", "org.openorb.orb.core.ORBSingleton");
        props.put("openorb.client.bindings.discard_old", "true");
        return props;
    }

    public static org.omg.CORBA.Object resolveInitialNameSerivce(ORB orb, String ip, int port, boolean ssl, String sslOptions) {
        Version iiopVersion = new Version();
        iiopVersion.major = 1;
        iiopVersion.minor = 2;

        ProfileBody_1_1 body = new ProfileBody_1_1();
        body.host = ip;
        body.port = ((short) port);
        body.iiop_version = iiopVersion;
        body.object_key = new byte[]{78, 97, 109, 101, 83, 101, 114, 118, 105, 99, 101};

        if (ssl) {
            body.components = new TaggedComponent[1];
            TaggedComponent a = new TaggedComponent(20, getSSLOptions(sslOptions, port));
            body.components[0] = a;
        } else {
            body.components = new TaggedComponent[0];
        }

        Any any = orb.create_any();
        ProfileBody_1_1Helper.insert(any, body);
        byte[] profile = null;
        try {
            profile = resolveImpl(orb, any);
        } catch (Exception e) {
            logger.error("resolve naming catch exception", e);
        }

        IOR ior = new IOR("", new TaggedProfile[1]);
        ior.profiles[0] = new TaggedProfile(0, profile);

        return new ObjectStub(orb, ior);
    }

    public static OrbConfig buildOrbConfig(String hostIp) {
        try {
            // host ip should be xx.xx.xx.xx:xx
            OrbConfig config = new OrbConfig();
            String[] delims = hostIp.trim().split(":");
            if (delims.length == 2 && InetAddressValidator.getInstance().isValid(delims[0])) {
                config.setHostIp(delims[0]);
                int port = Integer.parseInt(delims[1]);
                config.setPort(port);
                config.setSSLMode(port == 10511);
                return config;
            }
            logger.error("orb host ip is not valid {}", hostIp);
        } catch (Exception ex) {
            logger.error("build orb config catch exception", ex);
        }

        throw new CorbaRequestAbnormalException("build orb config error");
    }

    private static byte[] resolveImpl(ORB orb, Any any) throws UnknownEncoding, InvalidTypeForEncoding, InvalidName {
        CodecFactory factory = (CodecFactory) orb.resolve_initial_references("CodecFactory");
        Codec codec = factory.create_codec(new Encoding((short) 0, (byte) 1, (byte) 2));
        return codec.encode_value(any);
    }

    private static byte[] getSSLOptions(String sslOptions, int port) {
        byte portLow = (byte) (port & 0xFF);
        byte portHigh = (byte) ((port & 0xFF00) >> 8);
        byte[] defaultOptions = {0, 0, 0, -1, 0, 0, portHigh, portLow};

        if ((sslOptions == null) || (sslOptions.length() != 16)) {
            return defaultOptions;
        }

        byte[] options = new byte[8];
        int i = 0;
        for (int j = 0; i < sslOptions.length(); j += 1) {
            try {
                String c = sslOptions.substring(i, i + 2);
                options[j] = ((byte) Short.parseShort(c, 16));
            } catch (NumberFormatException e) {
                logger.error("get ssl option catch exception", e);
                return defaultOptions;
            }
            i += 2;
        }

        return options;
    }

    private static String[] map2array(HashMap<String, String> args) {
        String[] result = new String[args.size()];
        Set<String> keys = args.keySet();

        int i = 0;
        for (String key : keys) {
            result[(i++)] = String.format("%s=%s", key, args.get(key));
        }
        return result;
    }

    private static Properties map2Properties(HashMap<String, String> props) {
        Properties result = new Properties();
        Set<String> keys = props.keySet();

        for (String key : keys) {
            result.put(key, props.get(key));
        }

        return result;
    }

    private static Logger logger = LoggerFactory.getLogger(OrbUtil.class);
}
