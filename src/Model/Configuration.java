/*
 * free (adj.): unencumbered; not under the control of others
 * Written by Seth Holtzman in 2024 and released into the public domain
 * with no warranty of any kind, either expressed or implied.
 * It probably won't make your computer catch on fire, or eat
 * your children, but it might.  Use at your own risk.
 *
 * Oh but what's a constant among friends?
 */
package Model;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;

public class Configuration implements JSONSerializable {
    private String addr;
    private int port;
    private int threads;
    private String blenderAddr;
    private int blenderPort;
    private String type;

    /**
     * Builds a configuration from a JSON object.
     * @throws InvalidObjectException if the configuration object is invalid.
     */
    public Configuration(JSONObject obj) throws InvalidObjectException
    {
        deserialize(obj);
    }

    @Override
    public String serialize() {
        return toJSONType().toJSON();
    }

    @Override
    public void deserialize(JSONType obj) throws InvalidObjectException {
        JSONObject config = null;

        if (!(obj instanceof JSONObject))
            throw new InvalidObjectException("Expected configuration object.");

        config = (JSONObject) obj;

        if (!config.containsKey("type")) {
            throw new InvalidObjectException("Config must have type key");
        }

        type = config.getString("type");

        switch(type) {
            case "blender" :
                if (!(config.containsKey("addr") && config.containsKey("port") && config.containsKey("threads"))) {
                    throw new InvalidObjectException("Config must have addr, port and threads for blender");
                }

                this.addr = config.getString("addr");
                this.port = config.getInt("port");
                this.threads = config.getInt("threads");
                break;
            case "jondo" :
                if (!(config.containsKey("addr") && config.containsKey("port") && config.containsKey("threads")) &&
                config.containsKey("blenderAddr") && config.containsKey("blenderPort")) {
                    throw new InvalidObjectException("Config must have addr, port threads, blenderAddr, and blenderPort");
                }

                this.addr = config.getString("addr");
                this.port = config.getInt("port");
                this.threads = config.getInt("threads");
                this.blenderAddr = config.getString("blenderAddr");
                this.blenderPort = config.getInt("blenderPort");

                break;
            default:
                throw new InvalidObjectException("type must be blender or jondo");
        }
    }

    @Override
    public JSONType toJSONType() {
        JSONObject obj = new JSONObject();

        switch(type) {
            case "blender":
                obj.put("type",type);
                obj.put("addr",addr);
                obj.put("port",port);
                obj.put("threads",threads);
                return obj;
            case "jondo":
                obj.put("type",type);
                obj.put("addr",addr);
                obj.put("port",port);
                obj.put("threads",threads);
                obj.put("blenderAddr",blenderAddr);
                obj.put("blenderPort",blenderPort);
                return obj;
            default:
                System.err.println("Type must be blender or jondo but got " + type);
                return null;
        }
    }

    public String getAddr() {
        return addr;
    }

    public int getPort() {
        return port;
    }

    public int getThreads() {
        return threads;
    }

    public String getBlenderAddr() {
        return blenderAddr;
    }

    public int getBlenderPort() {
        return blenderPort;
    }
}
