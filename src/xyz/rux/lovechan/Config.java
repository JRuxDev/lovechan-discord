/****************************************************************************
 * Copyright 2016 Jonathan Rux (a.k.a. JRuxDev or Rux)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ****************************************************************************/

package xyz.rux.lovechan;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONString;

import java.awt.*;
import java.io.Serializable;
import java.util.*;
import java.util.List;

public class Config implements Serializable, JSONString
{

    protected static final Map<String, Config> configMap = new HashMap<>();

    protected static final String WRONG_TYPE = "The object mapped to the provided key '%s' is not instance of %s!";
    protected static final String NULL_TYPE = "No object was found for the provided key '%s'!";
    protected static final String NOT_SERIALIZABLE = "The provided value for key '%s' is not Serializable!";
    protected static final String MISSING_FIELD = "Field '%s' is not defined!";
    protected static final String PLEASE_POPULATE = " Please populate %s with valid information and the correct type!";

    protected final Map<String, Object> map = new HashMap<>();
    protected final String name;

    protected transient boolean immutable;

    protected Config(String name, boolean immutable){
        this.immutable = immutable;
        this.name = name;
    }

    protected Config(Config config, boolean immutable){
        this.immutable = immutable;
        this.name = config.name;
        this.map.putAll(config.map);
    }

    public Config immutable(){
        return immutable ? this : new Config(this, true);
    }

    @Override
    public String toJSONString(){
        JSONObject mapObj = new JSONObject();
        JSONObject object = new JSONObject()
                .put("name", name)
                .put("map", mapObj);
        synchronized (map){
            map.forEach(mapObj::put);
        }
        return object.toString();
    }

    @Override
    public String toString(){
        return toJSONString();
    }

    /* -- Modifiers -- */

    public <Element extends Serializable> Object put(String key, Element object){
        checkImmutable();
        synchronized (map){
            if (object == null)
                return map.remove(key);
            return map.put(key, object);
        }
    }

    public Map<String, Object> putAll(Map<String, Object> map){
        Map<String, Object> payload = new LinkedHashMap<>();
        map.forEach((key, value) ->{
            try{
                if (value != null)
                    payload.put(key, put(key, (Serializable) value));
                else payload.put(key, put(key, null));
            }
            catch (ClassCastException e){
                throw new IllegalArgumentException(String.format(NOT_SERIALIZABLE, key));
            }
        });
        return payload;
    }

    public Map<String, Object> putAllFromJson(JSONObject object){
        return putAll(convertToMap(object));
    }

    public Object remove(String key){
        return put(key, null);
    }

    public Map<String, Object> removeAll(Collection<String> keys){
        Map<String, Object> payload = new LinkedHashMap<>();
        keys.forEach(key ->
                payload.put(key, remove(key)));
        return payload;
    }

    /* -- Getters -- */

    // has

    public boolean has(String key){
        return map.get(key) != null;
    }

    public boolean hasAll(String... keys){
        for (String key : keys)
            if (!has(key))
                return false;
        return true;
    }

    public <T> boolean hasType(String key, Class<T> clazz){
        return has(key) && map.get(key).getClass().isAssignableFrom(clazz);
    }

    public boolean hasAllTypes(Map<String, Class<?>> map){
        for (String key : map.keySet())
            if (!hasType(key, map.get(key)))
                return false;
        return true;
    }

    // get

    public Object get(String key){
        synchronized (map){
            Object o = map.get(key);
            if (o == null)
                throw new NullPointerException(String.format(NULL_TYPE, key));
            return o;
        }
    }

    public <R> R getR(String key){
        try{
            return (R) get(key);
        }
        catch (ClassCastException e){
            throw new IllegalComponentStateException(String.format(WRONG_TYPE, key, "<R>"));
        }
    }

    // Type specific get

    public String getString(String key){
        Object obj = get(key);
        if (obj instanceof String)
            return (String) obj;
        throw new IllegalComponentStateException(String.format(WRONG_TYPE, key, "String"));
    }

    public Boolean getBoolean(String key){
        Object obj = get(key);
        if (obj instanceof Boolean)
            return (boolean) obj;
        throw new IllegalComponentStateException(String.format(WRONG_TYPE, key, "Boolean"));
    }

    public Integer getInteger(String key){
        Object obj = get(key);
        if (obj instanceof Integer)
            return (int) obj;
        throw new IllegalComponentStateException(String.format(WRONG_TYPE, key, "Integer"));
    }

    public Long getLong(String key){
        Object obj = get(key);
        if (obj instanceof Long)
            return (Long) obj;
        throw new IllegalComponentStateException(String.format(WRONG_TYPE, key, "Long"));
    }

    public Float getFloat(String key){
        Object obj = get(key);
        if (obj instanceof Float)
            return (Float) obj;
        throw new IllegalComponentStateException(String.format(WRONG_TYPE, key, "Float"));
    }

    /* -- Checks -- */

    protected void checkImmutable(){
        if (immutable)
            throw new UnsupportedOperationException("You are not allowed to modify this Config!");
    }

    protected List<Object> convertToList(JSONArray array){
        List<Object> list = new LinkedList<>();
        for (int i = 0; i < array.length(); i++){
            Object current = array.get(i);
            if (current == null)
                list.add(null);
            else if (current instanceof JSONArray)
                list.add(convertToList((JSONArray) current));
            else if (current instanceof JSONObject)
                list.add(convertToMap((JSONObject) current));
            else
                list.add(current);
        }
        return list;
    }

    protected Map<String, Object> convertToMap(JSONObject object){
        Map<String, Object> objMap = new HashMap<>();
        for (String key : object.keySet()){
            if (object.isNull(key))
                objMap.put(key, null);
            else{
                Object obj = object.get(key);
                if (obj instanceof JSONObject)
                    objMap.put(key, convertToMap((JSONObject) obj));
                else if (obj instanceof JSONArray)
                    objMap.put(key, convertToList((JSONArray) obj));
                else
                    objMap.put(key, obj);
            }
        }
        return objMap;
    }

    /* -- Static Methods -- */

    public static Config getConfig(String name){
        if (!configMap.containsKey(name))
            configMap.put(name, new Config(name, false));
        return configMap.get(name);
    }

    public static Config addConfig(Config cfg){
        if (hasConfig(cfg.name))
            throw new IllegalArgumentException("There already is a Config instance mapped to '" + cfg.name + "'!");
        configMap.put(cfg.name, cfg);
        return cfg;
    }

    public static boolean hasConfig(String name){
        return configMap.containsKey(name);
    }

    public static boolean hasConfig(Config cfg){
        return configMap.containsValue(cfg);
    }

    public static boolean hasConfig(String key, Config value){
        return configMap.get(key) == value;
    }

}