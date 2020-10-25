<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <meta http-equiv="x-ua-compatible" content="ie=edge">
    <link href="https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/css/bootstrap.min.css"
          rel="stylesheet"
          integrity="sha384-MCw98/SFnGE8fJT3GXwEOngsV7Zt27NXFoaoApmYm81iuXoPkFOJwJ8ERdknLPMO"
          crossorigin="anonymous">
    <title> ${title} | Betacom</title>
</head>
<body>

<div class="container">

<div class="row">
    <div class="col-md-12 mt-1">
      <span class="float-right">
        <button class="btn btn-outline-warning" type="button" data-toggle="collapse"
                data-target="#editor" aria-expanded="false" aria-controls="editor">Add item</button>
        <a class="btn btn-outline-primary" href="/logout" role="button" aria-pressed="true">Log out</a>
      </span>
        <h1 class="display-4">
            <span class="text-muted">Welcome back </span>
            ${user}
            <span class="text-muted">!</span>
        </h1>
    </div>

    <div class="col-md-12 mt-1">
        <h2>Inventory:</h2>
        <#list items>
            <ul>
                <#items as name>
                    <li>${name}
                        <form action="/delete" method="post">
                            <div class="form-group">
                                <input type="hidden" name="owner" value="${user}">
                                <input type="hidden" name="name" value="${name}">
                            </div>
                            <button type="submit" class="btn btn-danger btn-sm">Delete item</button>
                        </form>
                    </li>
                </#items>
            </ul>
        <#else>
            <p>You don't have any items in your inventor.</p>
        </#list>
    </div>


    <div class="col-md-12 collapsable collapse clearfix" id="editor">
        <form action="/items" method="post">
            <div class="form-group">
                <input type="hidden" name="owner" value="${user}">
                <input type="text" name="name" placeholder="New item">
            </div>
            <button type="submit" class="btn btn-primary">Add item</button>

        </form>
    </div>


</div>
</div> <!-- .container -->

<script src="https://code.jquery.com/jquery-3.3.1.slim.min.js"
        integrity="sha384-q8i/X+965DzO0rT7abK41JStQIAqVgRVzpbzo5smXKp4YfRvH+8abtTE1Pi6jizo"
        crossorigin="anonymous"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.3/umd/popper.min.js"
        integrity="sha384-ZMP7rVo3mIykV+2+9J3UJ46jBk0WLaUAdn689aCwoqbBJiSnjAK/l8WvCWPIPm49"
        crossorigin="anonymous"></script>
<script src="https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/js/bootstrap.min.js"
        integrity="sha384-ChfqqxuZUCnJSK3+MXmPNIyE6ZbWh2IMqE241rYiqJxyMiZ6OW/JmZQ5stwEULTy"
        crossorigin="anonymous"></script>

</body>
</html>
