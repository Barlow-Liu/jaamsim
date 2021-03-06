/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2020 JaamSim Software Inc.
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
@VERSION@

in vec4 fragColor;

uniform float C;
uniform float FC;
in float interpZ;

//layout(location = 0) out vec4 output;
out vec4 outColour;

void main()
{
    if (interpZ < 0)
        discard;

    outColour = fragColor;

    gl_FragDepth = log(interpZ*C+1)*FC;
}
