# 스마트 공기청정기
블루투스를 사용하여 아두이노로 만든 공기 청정기 제어

# 사용기술
* 블루투스 : BluetoothSocket
* 파싱 : OpenWeatherMap, OpenAPI

# 아두이노
블루투스를 이용하여 1Byte 씩 받고 보냄
* Post Parameters
     * M0# : 수동모드
     * M1# : 자동모드
     * P0# : 전원끄기
     * P1# : 세기1
     * P2# : 세기2
     * P3# : 세기3
     * T1#~T9# : 타이머 (1시간 단위)
* Get Parameters
     * A(n)# : 자동모드로 n 세기로 작동중
     * C(n)# : 수동모드로 n 세기로 작동중

<pre>
<code>
Copyright 2018 Hyunjin Woo

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
</code>
</pre>
