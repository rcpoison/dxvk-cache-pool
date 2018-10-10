
$(document).ready(function () {

	$('li.entry a.entryCount').tooltip({
		content: function (callback) {
			var element = $(this);
			var baseName = $(this).parent().attr('data-baseName');
			$.ajax({
				type: 'GET',
				dataType: 'json',
				cache: true,
				url: '/signatureStats/' + baseName,
				beforeSend: function (xhr) {
					if ($(element).data('stats')) {
						callback(buildStatsTooltip(element));
						return false;
					}
					return true;
				},
				success: function (stats) {
					$(element).data('stats', stats);
					callback(buildStatsTooltip(element));
				}
			});
		}
	}
	);

});

function buildStatsTooltip(element) {
	var table = '<table class="stats"><tr><th>Signature count</th><th>Occurences</th></tr>';
	$.each($(element).data('stats'), function (k, v) {
		table += '<tr><td>' + v.signatureCount + '</td><td>' + v.occurences + '</td></tr>';
	});
	table += '</table';
	return table;
}