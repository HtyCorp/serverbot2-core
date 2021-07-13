FROM public.ecr.aws/lambda/provided:al2
COPY service.jar /opt/service.jar
COPY bootstrap.java.sh $LAMBDA_RUNTIME_DIR/bootstrap
RUN yum install -y java-11-amazon-corretto-headless
CMD [ "example.handler" ]
