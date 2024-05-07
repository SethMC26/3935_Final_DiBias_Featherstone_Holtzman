package Model;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.List;
import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

/**
 * Represents a voting object that can be serialized and deserialized.
 * This class includes functionality to handle the voting process in a distributed system,
 */
public class Vote implements JSONSerializable {
    private String voteId;
    private String question;
    private List<String> options = new ArrayList<>();
    private String selection;
    private List<String> results;
    private String voterId;
    private String timestamp;

    /**
     * Constructs a Vote object using the provided builder.
     *
     * @param builder the Builder instance containing the properties for the Vote object.
     */
    private Vote(Builder builder) {
        this.voteId = builder.voteId;
        this.question = builder.question;
        this.options = builder.options;
        this.selection = builder.selection;
        this.results = builder.results;
        this.voterId = builder.voterId;
        this.timestamp = builder.timestamp;
    }

    /**
     * Constructs a Vote object using the provided builder.
     *
     * @param builder the Builder instance containing the properties for the Vote object.
     */
    public Vote(JSONObject jsonObject) throws InvalidObjectException {
        System.out.println("JSON OBJECT: " + jsonObject.getFormattedJSON());
        deserialize(jsonObject);
    }

    /**
     * Returns the vote ID.
     *
     * @return the vote ID as a String.
     */
    public String getVoteId() {
        return voteId;
    }

    /**
     * Returns the question associated with this vote.
     *
     * @return the question as a String.
     */
    public String getQuestion() {
        return question;
    }

    /**
     * Returns the list of options for this vote.
     *
     * @return a list of strings representing the options.
     */
    public List<String> getOptions() {
        return options;
    }

    /**
     * Returns the selected option in this vote.
     *
     * @return the selected option as a String.
     */
    public String getSelection() {
        return selection;
    }

    public List<String> getResults() {
        return results;
    }

    /**
     * Serializes this Vote object into a JSON string.
     *
     * @return a JSON string representation of this Vote.
     */
    @Override
    public String serialize() {
        return this.toJSONType().getFormattedJSON();
    }

    /**
     * Deserializes a JSONType object into a Vote object, ensuring it contains the necessary fields.
     *
     * @param jsonType the JSONType to deserialize.
     * @throws InvalidObjectException if the provided JSONType is not a JSONObject or lacks necessary fields.
     */
    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONObject))
            throw new InvalidObjectException("JSONObject expected.");

        JSONObject jsonObject = (JSONObject) jsonType;

        // Required fields
        voteId = jsonObject.getString("voteId");

        // Optional fields
        if (jsonObject.containsKey("question")) {
            question = jsonObject.getString("question");
        }
        if (jsonObject.containsKey("options")) {
            JSONArray options = jsonObject.getArray("options");
            List<String> optionsList = new ArrayList<>();
            for (int i = 0; i < options.size(); i++) {
            optionsList.add(options.getString(i));
            }
            this.options = optionsList;
        }
        if (jsonObject.containsKey("selection")) {
            selection = jsonObject.getString("selection");
        }
        if (jsonObject.containsKey("results")) {
            JSONArray results = jsonObject.getArray("results");
            List<String> resultsList = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                resultsList.add(results.getString(i));
            }
            this.results = resultsList;
        }
        if (jsonObject.containsKey("voterId")) {
            voterId = jsonObject.getString("voterId");
        }
        if (jsonObject.containsKey("timestamp")) {
            timestamp = jsonObject.getString("timestamp");
        }
    }

    /**
     * Converts this Vote into a JSONObject.
     *
     * @return a JSONObject representing this Vote.
     */
    @Override
    public JSONType toJSONType() {
        JSONObject json = new JSONObject();
        json.put("voteId", voteId);
        if (question != null)
            json.put("question", question);
        if (options != null) {
            JSONArray options = new JSONArray();
            for (String option : this.options) {
                options.add(option);
            }
            json.put("options", options);
        }
        if (selection != null)
            json.put("selection", selection);
        if (results != null) {
            JSONArray results = new JSONArray();
            for (String result : this.results) {
                results.add(result);
            }
            json.put("results", results);
        }
        if (voterId != null)
            json.put("voterId", voterId);
        if (timestamp != null)
            json.put("timestamp", timestamp);
        return json;
    }

    /**
     * Builder class for creating Vote instances.
     */
    public static class Builder {
        private String voteId;
        private String question;
        private List<String> options;
        private String selection;
        private List<String> results;
        private String voterId;
        private String timestamp;

        public Builder(String voteId) {
            this.voteId = voteId;
        }

        public Builder setQuestion(String question) {
            this.question = question;
            return this;
        }

        public Builder setOptions(List<String> options) {
            this.options = options;
            return this;
        }

        public Builder setSelection(String selection) {
            this.selection = selection;
            return this;
        }

        public Builder setResults(List<String> results) {
            this.results = results;
            return this;
        }

        public Builder setVoterId(String voterId) {
            this.voterId = voterId;
            return this;
        }

        public Builder setTimestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Builds and returns the Vote object.
         *
         * @return the constructed Vote instance.
         */
        public Vote build() {
            return new Vote(this);
        }
    }
}