/**
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
 */

package xyz.rux.lovechan.config;

import org.json.JSONObject;
import xyz.rux.lovechan.Config;

public class LoginConfig extends Config{
    public LoginConfig(JSONObject data, boolean immutable){
        super("login", false);
        super.putAllFromJson(data);
        super.immutable =immutable;
        if (!super.has("token"))
            throw new IllegalStateException(String.format(MISSING_FIELD + PLEASE_POPULATE, "token", "token"));
    }
}
