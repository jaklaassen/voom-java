package com.livefyre.voom.codec.vnd;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map.Entry;

import javax.mail.internet.ParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.livefyre.voom.VoomHeaders;
import com.livefyre.voom.VoomMessage;
import com.livefyre.voom.VoomMessagePart;
import com.livefyre.voom.codec.impl.MessageCodec;
import com.livefyre.voom.codec.TypeCodec;
import com.livefyre.voom.codec.json.JsonUtils;

public class LivefyreJsonMessageCodec<T> extends MessageCodec<JSONArray, T> {

    public LivefyreJsonMessageCodec(TypeCodec<JSONArray, T> partCodec) {
        super(partCodec);
    }

    @Override
    public byte[] encodeMessage(VoomMessage<T> msg) throws IOException {
        try {
            return encodeMessageInternal(msg);
        } catch (JSONException e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }
    
    private byte[] encodeMessageInternal(VoomMessage<T> msg) throws IOException, JSONException {
        JSONArray container = new JSONArray();
        JSONObject headers = new JSONObject();
        
        for (Entry<String, String> header:msg.getHeaders().toMap().entrySet()) {
            headers.put(header.getKey(), header.getValue());
        }
        headers.put("Content-Type", "message/vnd.livefyre+json");
        container.put(headers);
        
        for (VoomMessagePart<T> part: msg.getMessages()) {
            container.put(encodePart(part));
        }
        
        // Ensure the proper encoding here. See LF-7124.
        StringWriter sw = new StringWriter();
        return container.write(sw).toString().getBytes("UTF-8");
    }
    
    @Override
    public VoomMessage<T> decodeMessage(byte[] data) throws IOException {
        try {
            return decodeMessageInternal(data);
        } catch (ParseException | JSONException e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }
        
    public VoomMessage<T> decodeMessageInternal(byte[] data) throws IOException, JSONException, ParseException {
        JSONArray decoded = new JSONArray(new String(data));
        VoomHeaders headers = JsonUtils.getHeaders( decoded.getJSONObject(0));        
        VoomMessage<T> msg = new VoomMessage<T>(headers);
        
        for (int idx=1; idx<decoded.length(); idx++) {
            VoomHeaders partHeaders = JsonUtils.getHeaders(decoded.getJSONArray(idx).getJSONObject(0));
            msg.addMessage(decodePart(partHeaders.contentType(), decoded.getJSONArray(idx)));
        }
        return msg;
    }
}
