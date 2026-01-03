import os

import flask
import json
import logging
import numpy as np
import pandas as pd
import tensorflow as tf
import re

from tensorflow.keras.utils import to_categorical
from tensorflow.keras.callbacks import ReduceLROnPlateau, EarlyStopping
from sklearn.model_selection import train_test_split
from typing import Dict, List
from dataclasses import dataclass

BATCH_SIZE = 128
EPOCHS = 12
TRAIN_VALIDATION_SPLIT = 0.1
NUM_CATEGORIES = 73
NUM_NEURONS = 100
BASE = 0
OUTPUT_DIM = 2208
DROPOUT = 0.2
TEXT_VECTORIZATION_VOCABULARY_SIZE = 6000
CORPUS_DIRECTORY = 'data'

@dataclass(init=False)
class Ai:
    model = None
    labels: Dict = None

###########################
#### SERVICE FUNCTIONS ####
###########################
    
def load_training_data_from_csv(dataset_name: str):
    return pd.read_csv(os.path.join(CORPUS_DIRECTORY, dataset_name), delimiter=';').sample(frac=1)

def save_categories(filename, categories):
    with open(filename, 'w') as file:
        file.write(json.dumps(categories))

def load_categories(filename):
    with open(filename) as file:
        categories = json.loads(file.read())
    return categories


def create_model(train_dataset):
    encoder = tf.keras.layers.TextVectorization(max_tokens=TEXT_VECTORIZATION_VOCABULARY_SIZE)
    encoder.adapt(train_dataset.map(lambda text, label: text))

    model = tf.keras.Sequential([
        encoder,
        tf.keras.layers.Embedding(input_dim=len(encoder.get_vocabulary()), output_dim=OUTPUT_DIM, mask_zero=False),
        tf.keras.layers.Bidirectional(tf.keras.layers.LSTM(NUM_NEURONS)),
        tf.keras.layers.Dropout(DROPOUT),
        tf.keras.layers.Dense(NUM_CATEGORIES, activation='softmax')
    ])
    print(model.summary())
    return model


def concat_tokens(tags: str, ga_txt_cleaned: str):
    tag_List: List = str(tags).strip().split('|')
    ga_List: List = str(ga_txt_cleaned).strip().split(' ')
    tag_List.extend(ga_List)
    return ' '.join(tag_List)


def build_label_dict(df_dataset):
    print(df_dataset)
    df_dataset['matildaCategory'] = df_dataset['CATEGORY'].astype('category')
    return {value:idx for idx, value in enumerate(set(df_dataset['matildaCategory'].tolist())) }


def prepare_training_datasets(df_train_dataset, labels):
    df_train_dataset['matildaCategory'] = df_train_dataset['CATEGORY'].astype('category')
    df_train_dataset['matildaCategory_label'] = df_train_dataset['matildaCategory'].apply(lambda x: labels[x])
    df_train_dataset['token_list'] = df_train_dataset.apply(lambda x: concat_tokens(x.TAGS, x.ga_txt_cleaned), axis=1)
    df_data_token_train, df_data_token_valid, df_data_label_train, df_data_label_valid = train_test_split(np.array(df_train_dataset['token_list']), np.array(df_train_dataset['matildaCategory_label'].values.tolist()), test_size=TRAIN_VALIDATION_SPLIT)

    df_data_label_train = to_categorical(df_data_label_train)
    df_data_label_valid = to_categorical(df_data_label_valid)

    dataset_train = tf.data.Dataset.from_tensor_slices((df_data_token_train, df_data_label_train)).batch(BATCH_SIZE).prefetch(tf.data.AUTOTUNE)
    dataset_valid = tf.data.Dataset.from_tensor_slices((df_data_token_valid, df_data_label_valid)).batch(BATCH_SIZE).prefetch(tf.data.AUTOTUNE)

    return dataset_train, dataset_valid


def train(model, df_data_train, df_data_valid):
    reduce_lr = ReduceLROnPlateau(monitor='val_loss', factor=0.1, patience=3, verbose=1)
    early_stopping = EarlyStopping(monitor='val_loss', min_delta=0, patience=5, verbose=1)

    model.compile(loss=tf.keras.losses.CategoricalCrossentropy(), optimizer=tf.keras.optimizers.Adam(), metrics=["accuracy"])

    model.fit(df_data_train, shuffle=True, validation_data=df_data_valid,
              batch_size=BATCH_SIZE, epochs=EPOCHS, callbacks=[reduce_lr, early_stopping], verbose=True)

    return model

def preprocessing_filter_words(df):
    # including country-codes, orga/company-names, single chars, host-platform, other typical group-id main-domains
    removable_words_list = ['com', 'org', 'net', 'eu', 'jp', 'de', 'it', 'ru', 'fi', 'nl', 'ow', 'co', 'to', 'es', 'un', 'cm', 'ju', 'jy',
                            'fr', 'en', 'cn', 'sf', 'au', 'at', 'br', 'ch', 'ca', 'za', 'jj', 'nz', 'se', 'li', 'xj', 'io', 'ra', 'bw', 'of',
                            'ro', 'kz', 'uk', 'cd', 'us', 'me', 'info', 'edu', 'rc', ' rc', 'ac', 'be', 'lv', 'in', 'no', 'cj', 'um', 'fs',
                            'gu', 'im', 'I', 'as', 'pl', 'hu', 'cz', 'my', 'sk', 'ga', 'ib', 'lib', 'jar', 'dev', 'biz', 'top', 'kim',
                            'impl', 'apache', 'googlecode', 'github', 'git', 'codehaus', 'eclipse', 'sun', 
                            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 
                            's', 't', 'u', 'v', 'w', 'x', 'y', 'z', ' m']     
    ulist = []
    for word in df.ID.split(): 
        if word not in ulist and word not in removable_words_list:
            ulist.append(word.strip())
    return ' '.join(str(x) for x in ulist)

def preprocess_ds(df_data):
    df_data['GA'] = df_data['ID']
    
    # 1) Remove Nan/na
    df_data['ID'] = df_data['ID'].dropna()
    df_data['ID'].fillna(value='', inplace=True)
    
    # 2) Clean texts unwanted chars
    repl_list = {r'\.': ' ', 
                 ':': ' ',
                 '_': ' ',
                 '-': ' ',
                 'Â': '',
                'ð,,¼,Ã,°,Â,Ÿ,Ã,Ÿ,Ž,‰,,,Ã,°,Â,Ÿ,Â,,Â,¼,ï,ï': '',
                '[,#,&,$,*,{,},],\"': ''}
    df_data['ID'].replace(repl_list, regex=True, inplace=True)

    # 3) Replace all numbers
    for i in range(10):
        df_data['ID']=df_data['ID'].str.replace(str(i),'')
    
    # 4) Strip spaces
    df_data['ID'] = df_data['ID'].apply(lambda x: ' '.join(re.sub('([A-Z][a-z]+)', r' \1', re.sub('([A-Z]+)', r' \1', x)).split()).strip().lower())
    
    # 5) Remove blacklisted words
    df_data['ga_txt_cleaned'] = df_data.apply(preprocessing_filter_words, axis=1).astype(str)
    
    return df_data

def preprocess_ga(ga):
    removable_words_list = ['com', 'org', 'net', 'eu', 'jp', 'de', 'it', 'ru', 'fi', 'nl', 'ow', 'co', 'to', 'es', 'un', 'cm', 'ju', 'jy',
                            'fr', 'en', 'cn', 'sf', 'au', 'at', 'br', 'ch', 'ca', 'za', 'jj', 'nz', 'se', 'li', 'xj', 'io', 'ra', 'bw', 'of',
                            'ro', 'kz', 'uk', 'cd', 'us', 'me', 'info', 'edu', 'rc', ' rc', 'ac', 'be', 'lv', 'in', 'no', 'cj', 'um', 'fs',
                            'gu', 'im', 'I', 'as', 'pl', 'hu', 'cz', 'my', 'sk', 'ga', 'ib', 'lib', 'jar', 'dev', 'biz', 'top', 'kim',
                            'impl', 'apache', 'googlecode', 'github', 'git', 'codehaus', 'eclipse', 'sun', 
                            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 
                            's', 't', 'u', 'v', 'w', 'x', 'y', 'z', ' m']  
    
    # 1) Clean texts unwanted chars
    repl_list = {r'.': ' ',
                 ':': ' ',
                 '_': ' ',
                 '-': ' ',
                 'Â': '',
                'ð,,¼,Ã,°,Â,Ÿ,Ã,Ÿ,Ž,‰,,,Ã,°,Â,Ÿ,Â,,Â,¼,ï,ï': '',
                '[,#,&,$,*,{,},],\"': ''}
    for key, value in repl_list.items():
        ga = ga.replace(key, value)
    
    # 2) Replace all numbers
    for i in range(10):
        ga = ga.replace(str(i),'')
    
    # 4) Strip spaces
    ga = ' '.join(re.sub('([A-Z][a-z]+)', r' \1', re.sub('([A-Z]+)', r' \1', ga)).split()).strip().lower()
    
    # 5) Remove blacklisted words    
    ulist = []
    for word in ga.split(): 
        #word_replace = lemmatize(word)
        #word = word if word_replace is None else word_replace
        if word not in ulist and word not in removable_words_list:
            ulist.append(word.strip())
    
    return ' '.join(str(x) for x in ulist)

def train_and_save_model():
    # create and train model
    df_dataset = load_training_data_from_csv('export_gaManualTagsEntryList_20210511-final-V5-modified-training-set.csv')
    Ai.labels = build_label_dict(df_dataset)
    df_dataset = preprocess_ds(df_dataset)
    df_data_train, df_data_valid = prepare_training_datasets(df_dataset, Ai.labels)
    model = create_model(df_data_train)
    model = train(model, df_data_train, df_data_valid)
    print('--- FINISHED Preparation of model - finished training ---------------------------------------')
    return model


def get_prediction():
    data = {"success": False}
    params = flask.request.args

    # if parameters are found, return a prediction
    if (params != None):
        request_GA = params['ga']
        request_tokens = params['token']
        splitted_token = preprocess_ga(request_GA)
        tag_token = ' '.join(request_tokens.split("|"))
        token_list = f"{splitted_token} {tag_token}"
        prediction = Ai.model.predict(["'" + token_list + "'"])

        data = {"success": True}
        data["token"] = token_list
        data["prediction"] = list(Ai.labels.keys())[list(Ai.labels.values()).index(prediction.argmax())]
        data["percent"] = json.dumps(prediction.max().item())

    return data

Ai.model = train_and_save_model()

###########################
###### FLASK-SERVICE ######
###########################
app = flask.Flask(__name__)
logging.basicConfig(level=logging.DEBUG)

@app.route("/predict", methods=["GET"]) # HOST:PORT/predict?ga=xxx:yyy&token=x|y|z
def predict():
    app.logger.info(flask.request.args)
    return flask.jsonify(get_prediction())

@app.route("/health", methods=["GET"]) # HOST:PORT/predict?ga=xxx:yyy&token=x|y|z
def health():
    return '{"status": "OK!"}'


if __name__ == "__main__":
    app.run(host='0.0.0.0') # start the flask app, allow remote connections (for debug add: debug=True)