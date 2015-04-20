A demo CallChain tenta demonstrar como a cadeia de chamadas pode ser usada como 
meio de validação para permitir certas operações. Esta demo conta com um 
servidor, um proxy e um cliente. O servidor aceita mensagens, mas apenas se a 
última entidade da cadeia ("caller") for a mesma entidade (caso do proxy). O 
proxy aceita mensagens de qualquer um, e repassa a mensagem para o servidor.

------------------------------
-------- DEPENDÊNCIAS---------
------------------------------

As dependências de software são fornecidas já compiladas, em conjunto com a 
demo.

ant-1.8.2.jar
ant-launcher-1.8.2.jar
jacorb-3.5.jar
jacorb-omgapi-3.5.jar
openbus-sdk-core-2.1.0.0-RC1.jar
openbus-sdk-demo-util-2.1.0.0-RC1.jar
openbus-sdk-legacy-2.1.0.0-RC1.jar
openbus-sdk-assistant-core-2.1.0.0-RC1.jar
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

Proxy
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


------------------------------
---------- EXECUÇÃO ----------
------------------------------

Para que a demo funcione, pode ser necessário que as devidas permissões sejam 
cadastradas no barramento. Consulte o administrador do barramento e altere o 
arquivo admin\demo_callchain.adm conforme necessário.

A demo deve ser executada na seguinte ordem:

1) Servidor
2) Proxy
3) Cliente


-------------------------------
----------- EXEMPLO -----------
-------------------------------
Supondo que os jars que o demo depende estão em um diretório chamado 'lib':

1) java -Djava.endorsed.dirs=./lib/ -cp $(echo lib/*.jar | tr ' ' ':'):openbus-sdk-demo-callchain-2.1.0.0-RC1.jar demo.CallChainServer localhost 2089 demo_callchain_java DemoCallChain.key

2) java -Djava.endorsed.dirs=./lib/ -cp $(echo lib/*.jar | tr ' ' ':'):openbus-sdk-demo-callchain-2.1.0.0-RC1.jar demo.CallChainProxy localhost 2089 demo_callchain_java DemoCallChain.key

3) java -Djava.endorsed.dirs=./lib/ -cp $(echo lib/*.jar | tr ' ' ':'):openbus-sdk-demo-callchain-2.1.0.0-RC1.jar demo.CallChainClient localhost 2089 demo_callchain_java_client minhasenha
