/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.model.internal.registry;

import org.gradle.api.Action;
import org.gradle.api.Nullable;
import org.gradle.model.InvalidModelRuleException;
import org.gradle.model.ModelRuleBindingException;
import org.gradle.model.internal.core.ModelPath;
import org.gradle.model.internal.core.ModelReference;
import org.gradle.model.internal.core.rule.describe.ModelRuleDescriptor;
import org.gradle.model.internal.report.AmbiguousBindingReporter;
import org.gradle.model.internal.type.ModelType;

class OneOfTypeBinderCreationListener extends ModelBinding {
    private final Action<? super ModelNodeInternal> bindAction;
    private final ModelPath scope;

    public OneOfTypeBinderCreationListener(ModelRuleDescriptor descriptor, ModelReference<?> reference, ModelPath scope, boolean writable, Action<? super ModelNodeInternal> bindAction) {
        super(descriptor, reference, writable);
        this.bindAction = bindAction;
        this.scope = scope;
    }

    @Override
    public ModelPath getScope() {
        return scope;
    }

    @Nullable
    @Override
    public ModelType<?> getType() {
        return reference.getType();
    }

    public boolean onCreate(ModelNodeInternal node) {
        ModelRuleDescriptor creatorDescriptor = node.getDescriptor();
        ModelPath path = node.getPath();
        if (boundTo != null) {
            throw new InvalidModelRuleException(referrer, new ModelRuleBindingException(
                    new AmbiguousBindingReporter(reference, boundTo.getPath(), boundTo.getDescriptor(), path, creatorDescriptor).asString()
            ));
        } else {
            boundTo = node;
            bindAction.execute(node);
            return false; // don't unregister listener, need to keep listening for other potential bindings
        }
    }
}
