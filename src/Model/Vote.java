package Model;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.List;
import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

public class Vote implements JSONSerializable {
    private String voteId;
    private String question;
    private List<String> options = new ArrayList<>();
    private String selection;
    private String voterId;
    private String timestamp;

    private Vote(Builder builder) {
        this.voteId = builder.voteId;
        this.question = builder.question;
        this.options = builder.options;
        this.selection = builder.selection;
        this.voterId = builder.voterId;
        this.timestamp = builder.timestamp;
    }

    public Vote(JSONObject jsonObject) throws InvalidObjectException {
        System.out.println("JSON OBJECT: " + jsonObject.getFormattedJSON());
        deserialize(jsonObject);
    }

    public String getVoteId() {
        return voteId;
    }

    public String getQuestion() {
        return question;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getSelection() {
        return selection;
    }

    @Override
    public String serialize() {
        return this.toJSONType().getFormattedJSON();
    }

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
        if (jsonObject.containsKey("voterId")) {
            voterId = jsonObject.getString("voterId");
        }
        if (jsonObject.containsKey("timestamp")) {
            timestamp = jsonObject.getString("timestamp");
        }
    }

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
        if (voterId != null)
            json.put("voterId", voterId);
        if (timestamp != null)
            json.put("timestamp", timestamp);
        return json;
    }

    public static class Builder {
        private String voteId;
        private String question;
        private List<String> options;
        private String selection;
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

        public Builder setVoterId(String voterId) {
            this.voterId = voterId;
            return this;
        }

        public Builder setTimestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Vote build() {
            return new Vote(this);
        }
    }
}