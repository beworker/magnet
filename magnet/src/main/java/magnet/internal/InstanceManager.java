/*
 * Copyright (C) 2018 Sergej Shafarenka, www.halfbit.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package magnet.internal;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/* Subject to change. For internal use only. */
interface InstanceManager {

    <T> @Nullable InstanceFactory getInstanceFactory(
        Class<T> instanceType, String classifier, Class<InstanceFactory<T>> factoryType);

    <T> @Nullable InstanceFactory<T> getFilteredInstanceFactory(
        Class<T> type, String classifier, FactoryFilter factoryFilter);

    <T> List<InstanceFactory<T>> getManyInstanceFactories(
        Class<T> type, String classifier, FactoryFilter factoryFilter);

}