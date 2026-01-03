#3.9.7-alpine
FROM python:3.9
WORKDIR /ki

# copy the dependencies file to the working directory
COPY requirements.txt .

# install dependencies
RUN /usr/local/bin/python -m pip install --upgrade pip
RUN pip install -r requirements.txt

# copy the content of the local src directory to the working directory
COPY src/ .
COPY data/ ./data/

EXPOSE 8761

# command to run on container start
CMD [ "python", "./libsim_hybrid_service.py" ]