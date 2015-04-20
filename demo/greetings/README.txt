A demo Greetings tenta demonstrar o uso de vários componentes e facetas. O 
servidor cria 3 componentes, um para cada língua suportada (inglês, espanhol e 
português). Cada componente tem 3 facetas: uma responde "bom dia", uma "boa 
tarde" e outra "boa noite", na língua que estiver em uso. Apenas uma 
implementação do servant da interface Greetings é necessária.

O cliente pergunta ao usuário em qual língua deseja obter saudações. Se nenhuma 
língua for especificada, tentará obter saudações em todas. De acordo com o 
horário local, ele então utiliza a faceta adequada para obter a saudação 
correta.

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

Cliente
1) host do barramento
2) porta do barramento
3) nome de entidade
4) senha (opcional - se não for fornecida, será usado o nome de entidade)
5) domínio (opcional - se não for fornecida, será usado o valor 'openbus')
6) linguagem (opcional - deve ser uma das opções: English, Spanish, Portuguese. 
Padrão é Portuguese)


------------------------------
---------- EXECUÇÃO ----------
------------------------------

Para que a demo funcione, pode ser necessário que as devidas permissões sejam 
cadastradas no barramento. Consulte o administrador do barramento e altere o 
arquivo admin\demo_greetings.adm conforme necessário.

A demo deve ser executada na seguinte ordem:

1) Servidor
2) Cliente


-------------------------------
----------- EXEMPLO -----------
-------------------------------
Supondo que os jars que o demo depende estão em um diretório chamado 'lib':

1) java -Djava.endorsed.dirs=./lib/ -cp $(echo lib/*.jar | tr ' ' ':'):openbus-sdk-demo-greetings-2.1.0.0-RC1.jar demo.GreetingsServer localhost 2089 demo_greetings_java DemoGreetings.key 

2) java -Djava.endorsed.dirs=./lib/ -cp $(echo lib/*.jar | tr ' ' ':'):openbus-sdk-demo-greetings-2.1.0.0-RC1.jar demo.GreetingsClient localhost 2089 demo_greetings_java_client minhasenha Spanish
