   
   
   
   var tableNamesJSON;
   var tableData = [];
   $(document).ready(function() {
      $.get("/showtables", function(data){
          tableNamesJSON = JSON.parse(data);
          for(i in tableNamesJSON){
              $('.topnav').append("<div onclick='clickedOnItem("+i+")'>"+tableNamesJSON[i].toUpperCase()+"</div>");
          }
      });
   });

   

   function clickedOnItem(index){
      var tableName = tableNamesJSON[index];
      $.get("/table?tablename="+tableName, function(data){
           tableData[tableName] = JSON.parse(data);
           var currentData = tableData[tableName];
           document.getElementById("mytable").innerHTML = "";
           $('#mytable').append('<tr>');
           for(x in currentData.cols){ console.log("Length is "+x.length);   $('#mytable').append("<th>"+currentData.cols[x].toUpperCase()+"</th>");}
           $('#mytable').append('</tr>');


           for(y in currentData.data){
             $('#mytable').append('<tr>');
             var dataArr = currentData.data[y];
             for(value in dataArr){
             $('#mytable').append('<td>'+dataArr[value]+'</td>');
             }
             $('#mytable').append('</tr>');
           }


      });
   }


   function clickedOnQuery(){
       var query = $('#query_input').val();
       $.post("/query",
           {
               message : "Good Morning",
               query: query,
           },
           function(data, status){
                         var currentData = JSON.parse(data);
                         document.getElementById("mytable").innerHTML = "";
                         $('#mytable').append('<tr>');
                         for(x in currentData.cols){ console.log("Length is "+x.length);   $('#mytable').append("<th>"+currentData.cols[x].toUpperCase()+"</th>");}
                         $('#mytable').append('</tr>');
                         for(y in currentData.data){
                           $('#mytable').append('<tr>');
                           var dataArr = currentData.data[y];
                           for(value in dataArr){
                           $('#mytable').append('<td>'+dataArr[value]+'</td>');
                           }
                           $('#mytable').append('</tr>');
                         }
           });
      }

 function uploadFile(file){
    var data = new FormData();
    data.append("fileData",file);
    data.append('username','Sushobh Nadiger!');
    data.append('city','Mumbai');

    $.ajax({
        url: '/upload',
        type: 'POST',
        data: data,
        cache: false,
        dataType: 'json',
        processData: false, // Don't process the files
        contentType: false, // Set content type to false as jQuery will tell the server its a query string request
        success: function(data, textStatus, jqXHR)
        {

           
            window.open("/","_self")

        },
        error: function(jqXHR, textStatus, errorThrown)
        {
            console.log(errorThrown);
            alert("Error");
            // Handle errors here
            console.log('ERRORS: ' + textStatus);
            // STOP LOADING SPINNER
        }
    });
 }     

 $('#fileinput').on('change', function () {
    var fileReader = new FileReader();
    fileReader.onload = function () {
      var data = fileReader.result.split(',')[1];  // data <-- in this var you have the file data in Base64 format
      uploadFile(data);
    };
    fileReader.readAsDataURL($('#fileinput').prop('files')[0]);
});








