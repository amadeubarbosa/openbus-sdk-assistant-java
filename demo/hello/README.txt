A demo Hello tenta demonstrar a aplicação mais simples possível. Um servidor 
fornece o servant da interface Hello e o cliente realiza uma chamada "sayHello" 
nesse servant.

------------------------------
-------- DEPENDÊNCIAS---------
------------------------------

As dependências de software são fornecidas já compiladas, em conjunto com a 
demo.

ant-1.8.2.jar
ant-launcher-1.8.2.jar
jacorb-3.5.jar
jacorb-omgapi-3.5.jar
openbus-sdk-core-2.0-SNAPSHOT.jar
openbus-sdk-demo-util-2.0-SNAPSHOT.jar
openbus-sdk-legacy-2.0-SNAPSHOT.jar
openbus-sdk-assistant-core-2.0.0-SNAPSHOT.jar
scs-core-1.2.1.3.jar
slf4j-api-1.7.6.jar
slf4j-jdk14-1.7.6.jar


------------------------------
--------- ARGUMENTOS ---------
------------------------------

Servidor
1) host do barramento
2) porta do barramento
3) nome de entidade
4) caminho para a chave privada

Cliente
1) host do barramento
2) porta do barramento
3) nome de entidade
4) senha (opcional - se não for fornecida, será usado o nome de entidade)


------------------------------
---------- EXECUÇÃO ----------
------------------------------

Para que a demo funcione, pode ser necessário que as devidas permissões sejam 
cadastradas no barramento. Consulte o administrador do barramento e altere o 
arquivo admin\demo_hello.adm conforme necessário.

A demo deve ser executada na seguinte ordem:

1) Servidor
2) Cliente


-------------------------------
----------- EXEMPLO -----------
-------------------------------
Supondo que os jars que o demo depende estão em um diretório chamado 'lib':

1) java -Djava.endorsed.dirs=./lib/ -cp $(echo lib/*.jar | tr ' ' ':'):openbus-sdk-demo-hello-2.0-SNAPSHOT.jar demo.HelloServer localhost 2089 demo_hello_java DemoHello.key

2) java -Djava.endorsed.dirs=./lib/ -cp $(echo lib/*.jar | tr ' ' ':'):openbus-sdk-demo-hello-2.0-SNAPSHOT.jar demo.HelloClient localhost 2089 demo_hello_java_client minhasenha
