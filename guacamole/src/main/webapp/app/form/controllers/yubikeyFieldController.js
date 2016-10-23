/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


/**
 * Controller for yubikey fields.
 */
angular.module('form').controller('yubikeyFieldController', ['$scope',
    function yubikeyFieldController($scope) {

        /**
         * The type to use for the input field. By default, the input field will
         * have the type 'yubikey', and thus will be masked.
         *
         * @type String
         * @default 'yubikey'
         */
        $scope.yubikeyInputType = 'password';

        /**
         * Returns a string which describes the action the next call to
         * toggleYubikey() will have.
         *
         * @return {String}
         *     A string which describes the action the next call to
         *     toggleYubikey() will have.
         */
        $scope.getToggleYubikeyHelpText = function getToggleYubikeyHelpText() {

            // If yubikey is hidden, toggleYubikey() will show the yubikey
            if ($scope.yubikeyInputType === 'yubikey')
                return 'FORM.HELP_SHOW_YUBIKEY';

            // If yubikey is shown, toggleYubikey() will hide the yubikey
            return 'FORM.HELP_HIDE_YUBIKEY';

        };

        /**
         * Toggles visibility of the field contents, if this field is a
         * yubikey field. Initially, yubikey contents are masked
         * (invisible).
         */
        $scope.toggleYubikey = function toggleYubikey() {

            // If yubikey is hidden, show the yubikey
            if ($scope.yubikeyInputType === 'yubikey')
                $scope.yubikeyInputType = 'text';

            // If yubikey is shown, hide the yubikey
            else
                $scope.yubikeyInputType = 'yubikey';

        };

    }]);
