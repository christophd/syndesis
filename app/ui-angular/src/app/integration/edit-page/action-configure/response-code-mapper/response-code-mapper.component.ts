/*
 * Copyright (C) 2016 Red Hat, Inc.
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

import {
  Component,
  ViewEncapsulation,
  OnDestroy, Input, Output, EventEmitter, OnChanges, OnInit
} from '@angular/core';

import {
  ResponseCodeMapper,
  CurrentFlowService,
  FlowPageService,
  ErrorResponseCode
} from '@syndesis/ui/integration/edit-page';
import {
  IntegrationSupportService,
  StandardizedError,
  Step
} from '@syndesis/ui/platform';
import { Subscription } from 'rxjs';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ENDPOINT } from '@syndesis/ui/store';

@Component({
  selector: 'syndesis-response-code-mapper',
  templateUrl: './response-code-mapper.component.html',
  encapsulation: ViewEncapsulation.None,
  styleUrls: ['./response-code-mapper.component.scss']
})
export class ResponseCodeMapperComponent implements OnChanges, OnDestroy, OnInit {
  form: FormGroup;
  errorCodes: any = [];

  flowErrors: StandardizedError[] = [];

  step: Step;

  loading = true;
  formValueChangeSubscription: Subscription;

  @Input()
  configuredProperties: ResponseCodeMapper = {
    httpResponseCode: 200,
    errorResponseCodes: [
      {
        status: '404',
        errors: ['NOT_FOUND']
      },
      {
        status: '500',
        errors: ['INTERNAL_SERVER_ERROR']
      }
    ]
  };
  @Input() valid: boolean;
  @Input() position: number;
  @Output() validChange = new EventEmitter<boolean>();
  @Output() configuredPropertiesChange = new EventEmitter<ResponseCodeMapper>();

  constructor(
    private currentFlowService: CurrentFlowService,
    public integrationSupportService: IntegrationSupportService,
    public flowPageService: FlowPageService,
    private fb: FormBuilder
  ) {
    // nothing to do
  }

  // this can be valid even if we can't fetch the form data
  initForm(
    configuredProperties?: ResponseCodeMapper,
  ): void {
    let configuredErrorCodes: ErrorResponseCode[] = undefined;
    const configuredErrorGroups = [];
    const configuredReturnCode = (configuredProperties && configuredProperties.httpResponseCode)
      ? configuredProperties.httpResponseCode
      : '';

    // build up the form array from the incoming values (if any)
    if (configuredProperties && configuredProperties.errorResponseCodes) {
      // TODO hackity hack
      if (typeof configuredProperties.errorResponseCodes === 'string') {
        configuredErrorCodes = JSON.parse(<any>configuredProperties.errorResponseCodes);
      } else {
        configuredErrorCodes = configuredProperties.errorResponseCodes;
      }

      for (const incomingErrorCode of configuredErrorCodes) {
        configuredErrorGroups.push(this.fb.group(incomingErrorCode));
      }
    }
    const preloadedReturnCode = this.fb.group({
      httpResponseCode: [configuredReturnCode, Validators.required]
    });

    let preloadedErrorCodes;
    if (configuredErrorGroups.length > 0) {
      preloadedErrorCodes = this.fb.array(configuredErrorGroups);
    } else {
      preloadedErrorCodes = this.fb.array([]);
    }

    this.errorCodes = preloadedErrorCodes;

    const formGroupObj = {
      httpResponseCode: preloadedReturnCode,
      errorCodes: preloadedErrorCodes
    };

    this.form = this.fb.group(formGroupObj);

    this.formValueChangeSubscription = this.form.valueChanges.subscribe(_ => {
      this.valid = this.form.valid;
      this.validChange.emit(this.valid);
    });

    this.loading = false;
  }

  ngOnChanges(changes: any) {
    if (!('position' in changes)) {
      return;
    }

    this.loading = true;

    // Fetch our form data
    this.initForm(this.configuredProperties);
  }

  ngOnInit(): void {
    this.step = this.currentFlowService.getStep(this.position);

    const groupedErrors: StandardizedError[][] = this.currentFlowService.getMiddleSteps()
                                  .filter(step => step.stepKind === ENDPOINT)
                                  .filter(step => step.action)
                                  .filter(step => {
                                    return step.action.descriptor && step.action.descriptor.standardizedErrors;
                                  })
                                  .map(step => step.action.descriptor.standardizedErrors);

    if (groupedErrors.length) {
      this.flowErrors = groupedErrors.reduce((previous, next) => {
        return Array.from(new Set([...previous, ...next]));
      });
    }
  }

  ngOnDestroy(): void {
    if (this.formValueChangeSubscription) {
      this.formValueChangeSubscription.unsubscribe();
    }
  }

  createFlowGroup(flowId: string): FormGroup {
    const group = {
      flow: flowId,
      condition: ['', Validators.required]
    };
    return this.fb.group(group);
  }

  get myMappings(): FormArray {
    return <FormArray>this.errorCodes;
  }

  isMapped(error: StandardizedError, index: number) {
    const mapped = this.myMappings.controls[index].get('errors').value;
    return mapped && mapped.find(name => name === error.name);
  }

  toggleErrorMapping(error: StandardizedError, index: number) {
    const mapped = this.myMappings.controls[index].get('errors').value;

    if (mapped) {
      if (this.isMapped(error, index)) {
        mapped.splice(mapped.indexOf(error.name), 1);
      } else {
        mapped.push(error.name);
      }

      this.myMappings.controls[index].get('errors').setValue(mapped);
    } else {
      this.myMappings.controls[index].get('errors').setValue([error.name]);
    }
  }

  onChange() {
    this.valid = this.form.valid;
    this.validChange.emit(this.valid);
    if (!this.valid) {
      return;
    }

    const formGroupObj = this.form.value;

    const formattedProperties: ResponseCodeMapper = {
      httpResponseCode: this.form.controls.httpResponseCode.get('httpResponseCode').value,
      errorResponseCodes: formGroupObj.errorCodes
    };

    this.configuredPropertiesChange.emit(formattedProperties);
  }
}
