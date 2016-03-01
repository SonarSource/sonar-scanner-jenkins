function switch51(table) {
  jQuery(table).find(".sonar-52, .sonar-53").not(".sonar-51").prop('disabled', true);
  jQuery(table).find(".sonar-51").removeAttr('disabled');
}

function switch52(table) {
  jQuery(table).find(".sonar-51, .sonar-53").not(".sonar-52").prop('disabled', true);
  jQuery(table).find(".sonar-52").removeAttr('disabled');
}

function switch53(table) {
  jQuery(table).find(".sonar-51, .sonar-52").not(".sonar-53").prop('disabled', true);
  jQuery(table).find(".sonar-53").removeAttr('disabled');
}

var processServerVersion = function() {
  var val = jQuery(this).val();
  var table = jQuery(this).closest('table.sonar-installation');

  switch (val) {
    case '5.1':
      switch51(table);
      break;
    case '5.2':
      switch52(table);
      break;
    case '5.3':
      switch53(table);
      break;
    default:
      break;
    }
};

jQuery().ready(function() {
    jQuery(".sonar-section").on("change", ".sonar-server-version", processServerVersion);
    jQuery(".sonar-server-version").each(processServerVersion);
});