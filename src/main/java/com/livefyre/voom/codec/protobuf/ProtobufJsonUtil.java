package com.livefyre.voom.codec.protobuf;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;
import com.livefyre.voom.ProtobufLoader;
import com.livefyre.voom.ProtobufLoader.ProtobufLoadError;

public class ProtobufJsonUtil {
    private ProtobufLoader loader;
    
    public ProtobufJsonUtil(ProtobufLoader loader) {
        super();
        this.loader = loader;
    }

    public JSONObject protobufToJson(Message input) throws JSONException {
        JSONObject result = new JSONObject();
        Descriptor desc = input.getDescriptorForType();
        
        for (FieldDescriptor field:desc.getFields()) {
        	if (field.isRepeated() && input.getRepeatedFieldCount(field) == 0) {
        		continue;
        	}
            if (!field.isRepeated() && !input.hasField(field)) {
            	continue;
            }
            result.put(field.getName(), getFieldValue(input, field));
            
        }
        return result;
    }
    
    private JSONArray getRepeatedFieldValue(Message msg, FieldDescriptor field) throws JSONException {
        JSONArray result = new JSONArray();
        
        for (int i=0; i<msg.getRepeatedFieldCount(field); i++) {
            result.put(getFieldValueNotRepeated(msg, field, msg.getRepeatedField(field, i)));
        }
        
        return result;
    }
    
    private Object getFieldValueNotRepeated(Message msg, FieldDescriptor field, Object value) throws JSONException {
        if (field.getJavaType() == JavaType.MESSAGE) {
            return protobufToJson((Message)value);
        }
        if (field.getJavaType() == JavaType.ENUM) {
        	return ((EnumValueDescriptor) value).getNumber();
        }
        return value;
    }
    
    private Object getFieldValue(Message msg, FieldDescriptor field) throws JSONException {
        if (field.isRepeated()) {
            return getRepeatedFieldValue(msg, field);
        }
        return getFieldValueNotRepeated(msg, field, msg.getField(field));
    }
    
    public Message jsonToProtobuf(JSONObject input, Class<? extends Message> msgType) throws JSONException, ProtobufLoadError, ClassNotFoundException {
        Message.Builder builder = loader.getProtoBuilder(msgType);
        Descriptor descr = builder.getDescriptorForType();
        
        for (FieldDescriptor field: descr.getFields()) {
            if (!input.has(field.getName())) {
                continue;
            }
            Object value = input.get(field.getName());
            
            if (field.getJavaType() == JavaType.MESSAGE) {
                Class<? extends Message> composedType = (Class<? extends Message>) loader.getProtoClass(field.getMessageType().getFullName());
                builder.setField(field, jsonToProtobuf((JSONObject)value, composedType));
                continue;
            }
            
            if (field.getJavaType() == JavaType.ENUM) {
            	builder.setField(field, field.getEnumType().findValueByNumber((int)value));
            	continue;
            }
            
            if (field.isRepeated()) {
                JSONArray arrValue = (JSONArray) value;
                for (int idx=0; idx<arrValue.length(); idx++) {
                    builder.addRepeatedField(field, arrValue.get(idx));
                }
                continue;
            }
            builder.setField(field, value);
        }
        
        return builder.buildPartial();
    }
}
