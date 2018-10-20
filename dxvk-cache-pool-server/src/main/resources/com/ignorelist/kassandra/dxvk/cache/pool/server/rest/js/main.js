
$(document).ready(function () {

	$('li.entry a.download,li.entry span.entryCount').tooltip({
		content: function (callback) {
			var element = $(this);
			var baseName = $(this).closest('li').attr('data-baseName');
			$.ajax({
				type: 'GET',
				dataType: 'json',
				cache: true,
				url: '/signatureStats/' + baseName,
				beforeSend: function (xhr) {
					if ($(element).closest('li').data('stats')) {
						callback(buildStatsTooltip(element));
						return false;
					}
					return true;
				},
				success: function (stats) {
					$(element).closest('li').data('stats', stats);
					callback(buildStatsTooltip(element));
				}
			});
		}
	}
	);

});

function buildStatsTooltip(element) {
	var table = '<table class="stats"><tr><th>Signees</th><th>Entries</th></tr>';
	$.each($(element).closest('li').data('stats'), function (k, v) {
		table += '<tr><td>' + v.signatureCount + '</td><td>' + v.entryCount + '</td></tr>';
	});
	table += '</table';
	return table;
}