'use strict';

/**
 * Settings user page controller.
 */
App.controller('SettingsUser', function($scope, $state, Restangular) {
  /**
   * Load users from server.
   */
  $scope.loadUsers = function() {
    Restangular.one('admin/list').get({ limit: 100 }).then(function(data) {
      $scope.users = data.users;
    });
  };
  
  $scope.loadUsers();
  
  /**
   * Edit a user.
   */
  $scope.editUser = function(user) {
    $state.transitionTo('settings.user.edit', { username: user.username });
  };
});