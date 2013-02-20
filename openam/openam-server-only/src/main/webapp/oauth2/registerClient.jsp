<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Client Registration</title>
    <script src="../js/jquery.js"></script>
	<script>
        $(document).ready(function () {
            //add buttons
            $('#add_url').click(function(){
                console.log("add_url Pressed");
                var options = $('#redirection_urls').prop('options');
                var value = $('#url').val();
                options[options.length] = new Option(value, value, false, false);
            });
            $('#add_scope').click(function(){
                console.log("add_scope Pressed");
                var options = $('#scopes').prop('options');
                var value = $('#scope').val();
                options[options.length] = new Option(value, value, false, false);
            });
            $('#add_default_scope').click(function(){
                console.log("add_default_scope Pressed");
                var options = $('#default_scopes').prop('options');
                var value = $('#default_scope').val();
                options[options.length] = new Option(value, value, false, false);
            });
            $('#add_display_name').click(function(){
                console.log("add_display_name Pressed");
                var options = $('#display_names').prop('options');
                var value = $('#display_name').val();
                options[options.length] = new Option(value, value, false, false);
            });
            $('#add_dispaly_description').click(function(){
                console.log("add_dispaly_description Pressed");
                var options = $('#display_descriptions').prop('options');
                var value = $('#display_description').val();
                options[options.length] = new Option(value, value, false, false);
            });

            //remove buttons
            $('#remove_url').click(function(){
                console.log("remove_url Pressed");
                $('#redirection_urls option:selected').remove();
            });
            $('#remove_scope').click(function(){
                console.log("remove_scope Pressed");
                $('#scopes option:selected').remove();
            });
            $('#remove_default_scope').click(function(){
                console.log("remove_default_scope Pressed");
                $('default_scopes option:selected').remove();
            });
            $('#remove_display_name').click(function(){
                console.log("remove_display_name Pressed");
                $('display_names option:selected').remove();
            });
            $('#remove_display_description').click(function(){
                console.log("remove_display_description Pressed");
                $('#display_descriptions option:selected').remove();
            });

              //form submit
            $('#client').submit(function(){
                console.log("client submit");
                var password1 = $('#client_password').val();
                var password2 = $('#client_password2').val();
                //password not the same
                if (password1 != password2){
                    $('#message').html("<b>Client password must be the same.<b><br><br>");
                    return false;
                }

                //check for empty required values
                var redirection_options = $('#redirection_urls').prop('options');
                if (redirection_options.length <= 0){
                    $('#message').html("<b>Redirection URLS are required.<b><br><br>");
                    return false;
                }
                var scopes_options = $('#default_scopes').prop('options');
                var default_scope_options = $('#default_scopes').prop('options');
                if (default_scope_options.length <= 0){
                    $('#message').html("<b>Default scopes are required.<b><br><br>");
                    return false;
                }
                var display_names_options = $('#display_names').prop('options');
                if (display_names_options.length <= 0){
                    $('#message').html("<b>Display name are required.<b><br><br>");
                    return false;
                }
                var display_descriptions_options = $('#display_descriptions').prop('options');
                if (display_descriptions_options.length <= 0){
                    $('#message').html("<b>Display descriptions are required.<b><br><br>");
                    return false;
                }
                var client_id = $('#client_id');
                if (client_id.length <= 0){
                    $('#message').html("<b>Client ID is required.<b><br><br>");
                    return false;
                }
                var realm = $('#realm');
                if (realm.length <= 0){
                    $('#message').html("<b>Realm is required.<b><br><br>");
                    return false;
                }


                //get the selection options as a single string
                var temp = $('#redirection_urls_string');
                for (var i= 0; i < redirection_options.length; i++){
                    temp.append(redirection_options[i].value+";");
                }

                var temp = $('#scopes_string');
                for (var i= 0; i < scopes_options.length; i++){
                    temp.append(scopes_options[i].value+";");
                }
                var temp = $('#default_scopes_string');
                for (var i= 0; i < default_scope_options.length; i++){
                    temp.append(default_scope_options[i].value+";");
                }
                var temp = $('#display_names_string');
                for (var i= 0; i < display_names_options.length; i++){
                    temp.append(display_names_options[i].value+";");
                }
                var temp = $('#display_descriptions_string');
                for (var i= 0; i < display_descriptions_options.length; i++){
                    temp.append(display_descriptions_options[i].value+";");
                }
            });
        });
	</script>
</head>
<body>
	<h1>Register a Client</h1>
	<form name="client" action="register_client" method="POST" id="client">
		<div id="message"></div>
        Realm:<br><input type="input" name="realm" value="" id="realm"/><br><br>
        Client ID:<br><input type="input" name="client_id" value="" id="client_id"/><br><br>
		Client Password:<br><input type="password" name="client_password" value="" id="client_password"/><br><br>
		Client Password: (confirm)<br><input type="password" name="client_password2" value="" id="client_password2"/><br><br>
		Client Type:<br><input type="radio" name="client_type" value="Confidential" checked />Confidential<br>
					 <input type="radio" name="client_type" value="Public"/>Public<br><br>
		Redirection URL(s):<br><select id="redirection_urls" multiple="single" name="redirection_urls"></select><input type="button" name="remove_url" id="remove_url" value="Remove"/><br>
						  <input type="text" name="url" value="" id ="url"/><input type="button" name="add_url" id="add_url" value="Add"/><br><br>
		Scope(s):<br><select id="scopes" multiple="single" name="scopes"></select><input type="button" name="remove_scope" id="remove_scope" value="Remove"/><br>
				  <input type="text" name="scope" id ="scope" value=""/><input type="button" name="add_scope" id="add_scope" value="Add"/><br><br>
		Default Scope(s):<br><select id="default_scopes" multiple="single" name="default_scopes"></select><input type="button" name="remove_default_scope" id="remove_default_scope" value="Remove"/><br>
						  <input type="text" name="default_scope" id ="default_scope" value=""/><input type="button" name="add_default_scope" id="add_default_scope" value="Add"/><br><br>
		Display Name(s):<br><select id="display_names" multiple="single" name="display_names"></select><input type="button" name="remove_display_name" id="remove_display_name" value="Remove"/><br>
						 <input type="text" name="display_name" id ="display_name" value=""/><input type="button" name="add_display_name" id="add_display_name" value="Add"/><br><br>
		Display Description(s):<br><select id="display_descriptions" multiple="single" name="display_descriptions"></select><input type="button" name="remove_display_description" id="remove_display_description" value="Remove"/><br>
						 		<input type="text" name="display_description" id ="display_description" value=""/><input type="button" name="add_dispaly_description" id="add_dispaly_description" value="Add"/><br><br>
		<!--
		Token Validation Type:<br><input type="radio" name="token_type" value="Bearer" checked />Bearer<br>
							   <input type="radio" name="token_type" value="MAC"/>MAC<br>
							   <input type="radio" name="token_type" value="SAML 2.0"/>SAML 2.0<br><br>
        -->
		<input type="Submit" name="save" id="submit"/>

        <!--used to transferdata -->

        <textarea hidden="true" name="redirection_urls" id="redirection_urls_string"></textarea>
        <textarea hidden="true" name="scopes" id="scopes_string"></textarea>
        <textarea hidden="true" name="default_scopes" id="default_scopes_string"></textarea>
        <textarea hidden="true" name="display_names" id="display_names_string"></textarea>
        <textarea hidden="true" name="display_descriptions" id="display_descriptions_string"></textarea>
						
	</form>
</body>
