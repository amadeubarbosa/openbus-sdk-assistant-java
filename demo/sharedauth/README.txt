A demo Shared Auth tenta demonstrar como se realiza o login por autenticação 
compartilhada. Um servidor fornece o servant da interface Hello e um cliente 
realiza uma chamada "sayHello" nesse servant. Além disso, esse cliente inicia o 
processo de login por autenticação, recebendo um objeto LoginProcess e um 
segredo, que codifica em um arquivo texto.

Um outro cliente, que fará o login por autenticação compartilhada, lê então esse 
arquivo para obter o objeto LoginProcess e o segredo. De posse desses dados, 
consegue realizar o login, procurar pelo serviço Hello e realizar também uma 
chamada "sayHello", com a mesma entidade mas um identificador de login 
diferente.

É importante notar que o tempo entre a iniciação do processo de login por 
autenticação compartilhada do primeiro cliente e o login de fato do segundo 
cliente deve ser menor que o tempo de lease. Caso contrário, o login expirará e 
uma exceção será recebida.

------------------------------
-------- DEPENDÊNCIAS---------
------------------------------

As dependências de software são fornecidas já compiladas, em conjunto com a 
demo.

ant-1.8.2.jar
ant-launcher-1.8.2.jar
jacorb-3.5.jar
jacorb-omgapi-3.5.jar
openbus-sdk-core-2.1.0.0.jar
openbus-sdk-demo-util-2.1.0.0.jar
openbus-sdk-legacy-2.1.0.0.jar
openbus-sdk-assistant-core-2.1.0.0.jar
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
5) domínio (opcional - se não for fornecida, será usado o valor 'openbus')
6) caminho para o arquivo onde serão escritos os dados da autenticação compartilhada (opcional - gera um arquivo no diretorio corrente)

Cliente SharedAuth:
1) host do barramento
2) porta do barramento
3) caminho para o arquivo com os dados da autenticação compartilhada (opcional 
- busca pelo arquivo padrão no diretório corrente)


------------------------------
---------- EXECUÇÃO ----------
------------------------------

Para que a demo funcione, pode ser necessário que as devidas permissões sejam cadastradas no barramento. Consulte o administrador do barramento e altere o arquivo admin\demo_sharedauth.adm conforme necessário.

A demo deve ser executada na seguinte ordem:

1) Servidor
2) Cliente
3) Cliente SharedAuth


-------------------------------
----------- EXEMPLO -----------
-------------------------------
Supondo que os jars que o demo depende estão em um diretório chamado 'lib':

1) java -Djava.endorsed.dirs=./lib/ -cp $(echo lib/*.jar | tr ' ' ':'):openbus-sdk-demo-hello-2.1.0.0.jar demo.HelloServer localhost 2089 demo_hello_java DemoHello.key

2) java -Djava.endorsed.dirs=./lib/ -cp $(echo lib/*.jar | tr ' ' ':'):openbus-sdk-demo-sharedauth-2.1.0.0.jar demo.Client localhost 2089 demo_sharedauth_java_client minhasenha

3) java -Djava.endorsed.dirs=./lib/ -cp $(echo lib/*.jar | tr ' ' ':'):openbus-sdk-demo-sharedauth-2.1.0.0.jar demo.SharedAuthClient localhost 2089 

