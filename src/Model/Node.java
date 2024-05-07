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
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Internal representation of a Model.Jondo
 */
public class Node implements JSONSerializable {
    private String addr;
    private int port;
    private String uid;

    /**
     * Creates a new Model.Jondo based on address and port
     *
     * @param _addr String IP address of Model.Jondo
     * @param _port Int Port of this Model.Jondo
     */
    public Node(String _addr, int _port) {
        addr = _addr;
        port = _port;

        //Create UID from addr and port
        createUID();
    }

    /**
     * Constructs a Node by deserializing a JSON object.
     *
     * @param jondoJSON the JSON object containing the node's data
     * @throws InvalidObjectException if the JSON object does not contain necessary fields
     */
    public Node(JSONObject jondoJSON) throws InvalidObjectException {
        deserialize(jondoJSON);
    }

    /**
     * Serializes this node into a JSON string.
     *
     * @return a JSON string representation of this node
     */
    @Override
    public String serialize() {
        return toJSONType().toJSON();
    }

    /**
     * Deserializes a JSONType object into a Node object, ensuring it contains the necessary fields.
     *
     * @param jsonType the JSONType to deserialize
     * @throws InvalidObjectException if the provided JSONType is not a JSONObject or lacks necessary fields
     */
    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONObject))
            throw new InvalidObjectException("JSONObject expected.");

        JSONObject jondoJSON = (JSONObject) jsonType;

        if (!(jondoJSON.containsKey("addr") || jondoJSON.containsKey("port"))) {
            throw new InvalidObjectException("JSONObject does not have TYPE field");
        }

        addr = jondoJSON.getString("addr");
        port = jondoJSON.getInt("port");

        //create UID based on addr and port
        createUID();
    }

    /**
     * Converts Model.Jondo(Node) to JSON object
     *
     * @return JSONObject which is the Model.Jondo
     */
    @Override
    public JSONType toJSONType() {
        JSONObject jondoJSON = new JSONObject();

        jondoJSON.put("addr",addr);
        jondoJSON.put("port",port);

        return jondoJSON;
    }

    /**
     * Gets IP address of Model.Jondo
     * @return String of IP address
     */
    public String getAddr() {
        return addr;
    }

    /**
     * Gets Port of Model.Jondo
     * @return int port
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets UID of this node which is 32 bit(4 byte) hash based on addr and port
     * @return String 32 bit(4 byte) hash
     */
    public String getUid() {
        return uid;
    }

    @Override
    public String toString() {
        return toJSONType().getFormattedJSON();
    }

    /**
     * Compute the nodes ID by taking the SHA-1 hash of the
     * IP address and port pair to determine the UID of this
     * node.
     *
     * From Proj 4 Written by Zachary Kissel
     */
    private void createUID()
    {
        ByteBuffer buff = ByteBuffer.allocate(4);
        buff.putInt(this.port);
        try
        {
            MessageDigest hash = MessageDigest.getInstance("SHA-1");
            hash.update(this.addr.getBytes());
            hash.update(buff.array());
            this.uid = Base64.getEncoder().encodeToString(hash.digest());
        }
        catch (NoSuchAlgorithmException e)
        {
            System.err.println("Internal Error: SHA1 hash not supported.");
        }
    }
}
