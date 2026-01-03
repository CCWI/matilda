import os
import csv
import json
from datetime import datetime
from typing import Dict, List, Tuple

from tensorflow import keras
import numpy as np
import pandas as pd
import flask
from sklearn.model_selection import train_test_split
from tensorflow.keras.callbacks import ReduceLROnPlateau, TensorBoard, EarlyStopping

NUM_NEURONS = 150
NUM_CATEGORIES = 73
EPOCHS = 75
BATCH_SIZE = 1024
TEST_SET_SIZE = 0.05

EXEC_TIMESTAMP = f'{datetime.now().strftime("%Y-%m-%d_%H-%M-%S")}/'
DATASET = os.path.abspath("data/export_gaManualTagsEntryList_20210511-final-V5-modified-training-set.csv")
LABELS_CATEGORY_CSV = os.path.abspath("logs/" + EXEC_TIMESTAMP + f"labels_category.csv")
TAGS_CATEGORY_CSV = os.path.abspath("logs/" + EXEC_TIMESTAMP + f"tags_category.csv")
LOG_DIR = os.path.abspath("logs/" + EXEC_TIMESTAMP)


class DataLoader(object):

    def __init__(self):
        self.LABEL_MAPPING: Dict = {}
        self.LABEL_INDEX = 0
        self.TAG_MAPPING: Dict = {}
        self.TAG_INDEX: int = 0
        self.unknown_labels = 0

    def _create_tag_index_(self, tags: str):
        tag_string: List = str(tags).strip().split('|')
        for i in range(len(tag_string)):
            tag = tag_string[i]
            if tag not in self.TAG_MAPPING:
                self.TAG_MAPPING[tag] = self.TAG_INDEX
                self.TAG_INDEX += 1

    def _label_to_category_number_(self, category: str):
        if category not in self.LABEL_MAPPING:
            self.LABEL_MAPPING[category] = self.LABEL_INDEX
            self.LABEL_INDEX += 1
        return self.LABEL_MAPPING.get(category)

    def _tags_to_vector_(self, tags: str):
        tag_strings: List = str(tags).strip().split('|')
        vector: List = [0] * len(self.TAG_MAPPING)  # [-1]
        for i in range(len(tag_strings)):
            try:
                tag = tag_strings[i]
                if tag not in self.TAG_MAPPING:
                    raise Exception(f"Found {len(tag_strings)} tags: {tags}")
                vector_index = self.TAG_MAPPING.get(tag)
                vector[vector_index] = 1
            except IndexError as e:
                raise Exception(f"Found {len(tag_strings)} tags: {tags}")
        return vector

    def read_data(self):
        dataset = pd.read_csv(DATASET, delimiter=';').sample(frac=1)
        cat_labels = dataset['CATEGORY'].apply(lambda category: self._label_to_category_number_(category)).tolist()

        print(f"Couldn't map {str(self.unknown_labels)} labels, mapped to 'unknown'")

        tag_data = dataset['TAGS']
        tag_data.apply(self._create_tag_index_)
        tag_vector = tag_data.apply(self._tags_to_vector_).tolist()

        for i in range(len(cat_labels)):
            cat_labels[i] = keras.utils.to_categorical(cat_labels[i], num_classes=self.LABEL_INDEX)

        with open(os.path.join(LOG_DIR, "mapping-taglist.csv"), "w") as f:
            w = csv.DictWriter(f, self.TAG_MAPPING.keys())
            w.writeheader()
            w.writerow(self.TAG_MAPPING)

        with open(os.path.join(LOG_DIR, "mapping-labels.csv"), "w") as f:
            w = csv.DictWriter(f, self.LABEL_MAPPING.keys())
            w.writeheader()
            w.writerow(self.LABEL_MAPPING)

        return np.array(cat_labels), np.array(tag_vector)

    def save_tags(self):
        df = pd.DataFrame(self.TAG_MAPPING, index=[0]).transpose()
        df.to_csv(TAGS_CATEGORY_CSV)

    def save_labels(self):
        df = pd.DataFrame(self.LABEL_MAPPING, index=[0]).transpose()
        df.to_csv(LABELS_CATEGORY_CSV)

    def get_matching_label(self, s_index: int):
        for name, index in self.LABEL_MAPPING.items():
            if index == s_index:
                return name


def create_model(input_shape: Tuple[int, int], neurons: int):
    inputs = keras.Input(shape=input_shape)
    hidden_layer1 = keras.layers.Dense(neurons, activation="relu")(inputs)
    flatten_layer = keras.layers.Flatten()(hidden_layer1)
    outputs = keras.layers.Dense(NUM_CATEGORIES, activation='softmax')(flatten_layer)
    lib_model = keras.Model(inputs=inputs, outputs=outputs, name="librarySimilarityModel")
    lib_model.summary()
    return lib_model


log_dir = LOG_DIR + f'/'
os.makedirs(log_dir)
dataLoader = DataLoader()

labels, tags = dataLoader.read_data()
tags_train, tags_test, labels_train, labels_test = train_test_split(tags, labels, test_size=TEST_SET_SIZE)

# PRINT INFORMATION
print("labels shape=", labels.shape)
print("tags shape=", tags.shape)
print(f"Found {dataLoader.LABEL_INDEX} labels and {dataLoader.TAG_INDEX} tags.")
print(f'Training selected model neurons={NUM_NEURONS}')
print(f'Testing on {len(tags_test)} samples.')

# CREATE MODEL-RELATED DATA
reduce_lr = ReduceLROnPlateau(monitor='val_loss', factor=0.1, patience=3, verbose=1)
logging = TensorBoard(log_dir=log_dir)
# early_stopping = EarlyStopping(monitor='val_loss', min_delta=0, patience=5, verbose=1)
# checkpoint = ModelCheckpoint(log_dir + 'model_check_point.h5', monitor='val_loss', save_weights_only=True, save_best_only=True, period=5)

# CREATE MODEL
model = create_model(input_shape=(dataLoader.TAG_INDEX,), neurons=NUM_NEURONS)
model.compile(
    loss=keras.losses.categorical_crossentropy,
    optimizer=keras.optimizers.Adam(),
    metrics=["accuracy"],
    weighted_metrics=['accuracy']
)

# TRAIN
history = model.fit(tags_train, labels_train,
                    batch_size=BATCH_SIZE,
                    epochs=EPOCHS,
                    validation_data=(tags_test, labels_test),
                    callbacks=[reduce_lr, logging]) #, early_stopping])

# SAVE model
model.save(os.path.join(log_dir, f"final_model.h5"))

#######################################################################################################################
#######################################################################################################################
#######################################################################################################################

# instantiate flask 
app = flask.Flask(__name__)


# LOAD model (not needed when training is done before start
# model = tf.keras.models.load_model(os.path.join(log_dir, f"final_model.h5"), custom_objects=None, compile=True)

def tags_to_vector(tags: str):
    tag_strings: List = str(tags).strip().split('|')
    vector: List = [0] * len(dataLoader.TAG_MAPPING)
    for i in range(len(tag_strings)):
        try:
            tag = tag_strings[i]
            if tag not in dataLoader.TAG_MAPPING:
                raise Exception(f"Found {len(tag_strings)} tags: {tags}")
            vector_index = dataLoader.TAG_MAPPING.get(tag)
            vector[vector_index] = 1
        except IndexError as e:
            raise Exception(f"Found {len(tag_strings)} tags: {tags}")
    return vector


@app.route("/predict", methods=["GET"])  # "GET","POST"
def predict():
    data = {"success": False}

    params = flask.request.json
    if params is None:
        params = flask.request.args

    if params is not None:  # if parameters are found, return a prediction
        request_tags = params['tags']
        vector = tags_to_vector(request_tags)
        prediction = model.predict([vector])

        data = {"success": True,
                "tags": request_tags,
                "prediction": dataLoader.get_matching_label(prediction.argmax()),
                "percent": json.dumps(prediction.max().item())}

    return flask.jsonify(data)


if __name__ == "__main__":
    app.run(host='0.0.0.0')
