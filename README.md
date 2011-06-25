# javamatic

> Good morning sir, I see that you're interested in adding a field to your class. Certainly, you are in the right office, let me get you an application form. Please fill it in in camel case. Yes, correct, apart from the messages section, remember to add spaces there. Oh I see there is an enumeration involved as well. All capital letters for that one I'm afraid. Are you done? Great, please sign here, here and here. I'll photocopy this and then you can take it up to the 3rd floor where you can get an application form for getters and setters. Sorry, I'm afraid the elevator is broken.

Javamatic is a tiny templating engine and library that allows you to generate the tedious parts of Java easily by defining ad hoc code templates. It is intended to be used from a Clojure REPL which you will keep open along with your usual Java IDE. You generate the Java code in the REPL by calling javamatic functions, code is placed into your clipboard and you paste it into your IDE. Javamatic is basically the half-way but practical answer to the Java programmer who knows Clojure (or some other Lisp) and keeps telling herself "This would be much quicker to write with macros (and less boring)!".

## Usage

### Basic usage

Say you have a class and you need to write a copy constructor for it. It becomes boring pretty quickly if the class has more than about 5 fields. For brevity, let's say it actually has the following 5 fields: firstName, surname, email, dayTelephone, mobileTelephone. You can generate the *body* of the copy constructor in the following way:

1. Fire up your REPL and load src/javamatic/core.clj
2. In the REPL, enter the following:

````clojure
    (print (copy (render-template
                  "this.set{{x}}(other.get{{x}());\n"
                  (qw FirstName Surname Email
                      DayTelephone MobileTelephone))))
````
					  
The `qw` macro removes the need for quotes, unless your input has spaces, in which case you can mix normal strings with the symbols given to `qw`. This is an idea stolen from Perl.

3. Evaluate the expression
4. The following code is printed and also placed in your clipboard:

    this.setFirstName(other.getFirstName());
    this.setSurname(other.getSurname());
    this.setEmail(other.getEmail());
    this.setDayTelephone(other.getDayTelephone());
    this.setMobileTelephone(other.getMobileTelephone());

### Processing input

Often you will need to generate code for a series of fields that are already declared in the class. In such cases, you can pass the code itself to the `names-from-declarations` function (remberer that Clojure allows multi-line strings):

    (print (copy (render-template
				  "this.set{{(capitalize x)}}(other.get{{(capitalize x)}}());\n"
				  (names-from-declarations
				  "
    private String firstName;
    private String surname;
    private String email;
    private String dayTelephone;
    private String mobileTelephone;"))))

This produces the same result as above. In a similar vein, the `first-alphas` allows you to extract the variable names from lines such as `firstName.set(null);`.

Also, as you can see in the example above, if the placeholder starts with parenthesis, it is evaluated as a Clojure expression. You can use any expression that you like, but javamatic provides a few string manipulation functions. See the *string manipulation* section of the source for a full list.

### The pastebox

In some cases, the Java code that you'd like to process and pass to extract the list of values for your template is not very convenient for inclusion in Clojure code because it contains characters that need escaping (such as quotes). To overcome that without having to manually fix the code, just call the `(pastebox)` function that will bring up a GUI box where you can paste the Java code and intern it into a var whose name you can also define. Alternatively, you can click on "copy literal", which will escape the code an convert it into a literal string that can be pasted directly into Clojure code (this will use actual new lines instead of \n).
	
## License

Copyright (C) 2011 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
