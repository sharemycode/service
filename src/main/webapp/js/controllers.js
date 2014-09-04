/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
function UserController($scope, $http, Users) {

    // Define a refresh function, that updates the data from the REST service
    $scope.refresh = function() {
        $scope.users = Users.query();
    };

    // Define a reset function, that clears the prototype newMember object, and
    // consequently, the form
    $scope.reset = function() {
        // clear input fields
        $scope.newUser = {};
    };

    // Define a register function, which adds the member using the REST service,
    // and displays any error messages
    $scope.register = function() {
        $scope.successMessages = '';
        $scope.errorMessages = '';
        $scope.errors = {};

        Users.save($scope.newUser, function(data) {

            // mark success on the registration form
            $scope.successMessages = [ 'User Registered' ];

            // Update the list of members
            $scope.refresh();

            // Clear the form
            $scope.reset();
        }, function(result) {
            if ((result.status == 409) || (result.status == 400)) {
                $scope.errors = result.data;
            } else {
                $scope.errorMessages = [ 'Unknown  server error' ];
            }
            $scope.$apply();
        });

    };

    // Call the refresh() function, to populate the list of members
    $scope.refresh();

    // Initialize newMember here to prevent Angular from sending a request
    // without a proper Content-Type.
    $scope.reset();

    // Set the default orderBy to the name property
    $scope.orderBy = 'username';
}

function ProjectController($scope, $http, Projects) {

    // Define a refresh function, that updates the data from the REST service
    $scope.refresh = function() {
        $scope.projects = Projects.query();
    };

    // Define a reset function, that clears the prototype newProject object, and
    // consequently, the form
    $scope.reset = function() {
        // clear input fields
        $scope.newProject = {};
    };

    // Define a create function, which adds the project using the REST service,
    // and displays any error messages
    // AngularJS does not yet have native support for multipart/form-data
    // Using standard HTML post request with multipart encoding instead
    //$scope.create = function() {
    
    // Call the refresh() function, to populate the list of projects
    $scope.refresh();

    // Initialize newProject here to prevent Angular from sending a request
    // without a proper Content-Type.
    $scope.reset();

    // Set the default orderBy to the name property
    $scope.orderBy = 'name';
}