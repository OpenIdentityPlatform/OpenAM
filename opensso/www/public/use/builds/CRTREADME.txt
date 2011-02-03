
v0.1 of CRT UI

General concept :

1) Display CRT form 
   (http://https://opensso.dev.java.net/public/use/builds/crtfinal.html) .
   Issue# must be filled in.

2) CRT created as a timestamped attachment to the issue entered.

3) CRT can be accessed by clicking on the link from issue view page.

4) The attachment contains both the form and text versions of the CRT -
   you can choose between the two.

5) The CRT attachment can be "changed" -> a new attachment is created.

6) Add clickable links for  fields that are accepted as urls.

7) A simple "Approve" form is displayed for approvers to communicate 
  Approve/reject/comment on the CRT.
  Approval is created as a attachment too.

  
TODOs:

1) Mandatory field logic is not in yet.

2) Owing to async nature of things, it takes 1-2 seconds for
   the "create attachement" operation to complete. 
   Need to add a "Wait...updating issue" message.

3) Misc links need  (migration, install help) need to be moved elsewhere
  so tha tthe popup doesnt contain teh entire UI. 

4) Provide backup facility for CRTs to be submitted without this tool -
  in case the tool breaks as a result of java.net issue tracker changes.


Design :
1) The CRT form itself is a truncated version of the one we use internally.

2) Current window scarping and AJAX is used to determine whether the
     use is logged in as well as the id to polulate the submitter and approver
     fields.
3) Submit : Javascript is used to concatenate all entered values into a 
   html form and ascii text. Fairly hairy code is needed since the submittal
   is not a regular "&" separated post - its a mutipart mime submittal.

4) AJAX is used to create the attachment by invoking the "add attachment" 
  issue tracker URL.
 
5) Returned response from the the "add attachment" URL is blindly displayed
  on the browser.

6) Since the attachment is already factored as a html form the CRT form
   can be accessed with the values intact for updates.

7) A "Approval" form is also displayed when the attachement is accessed - pre
  filled with the logged in user's id.
  The approver may "reject" the CRT (by not ticking the "Approve" checkbox)
  or may approve it by checking it.
  The "Approve" submit is also stored as a attachment using similar
  technique used in (3) thru (5) above.

8) Notes on crt.js :
   All javascript and AJAX is encapsulated in this file.
   In order to create the CRT html form inside the attachment (see (3) above)
   a templatized version of crtfinal.html is included as a string variable.
   
   Hence if any change is made to crtfinal.html, make sure crt.js is
   updated as follows :
     - remove newlines from crtfinal.html so that it is a single line.
     - escape embedded single quotes : "'" to "\'"
     - update the crttemplate variable in crt.js.
     - If a new element is added, 
       - make sure it is is tag swappable.
       - Change crtn array in aggSubmit() function for a human readable 
         form of the element.
 
   
--

